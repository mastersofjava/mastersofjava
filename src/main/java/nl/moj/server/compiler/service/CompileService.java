/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.compiler.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.config.properties.Languages;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

@Service
@Slf4j
public class CompileService {

    private CompileAttemptRepository compileAttemptRepository;
    private AssignmentStatusRepository assignmentStatusRepository;
    private CompetitionRuntime competition;
    private MojServerProperties mojServerProperties;
    private TeamService teamService;

    public CompileService(CompetitionRuntime competition, MojServerProperties mojServerProperties,
                          CompileAttemptRepository compileAttemptRepository, AssignmentStatusRepository assignmentStatusRepository,
                          TeamService teamService) {
        this.competition = competition;
        this.mojServerProperties = mojServerProperties;
        this.compileAttemptRepository = compileAttemptRepository;
        this.assignmentStatusRepository = assignmentStatusRepository;
        this.teamService = teamService;
    }

    public CompletableFuture<CompileResult> scheduleCompile(Team team, SourceMessage message, Executor executor) {
        // determine compiler version to use.
        var javaVersion = mojServerProperties.getLanguages()
                .getJavaVersion(competition.getActiveAssignment()
                        .getAssignmentDescriptor()
                        .getJavaVersion());

        // compile code.
        return CompletableFuture.supplyAsync(() -> javaCompile(javaVersion, team, message), executor);
    }


    private CompileResult javaCompile(Languages.JavaVersion javaVersion, Team team, SourceMessage message) {
        // TODO should not be here.
        ActiveAssignment state = competition.getActiveAssignment();
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(), state
                .getCompetitionSession(), team);

        CompileAttempt compileAttempt = CompileAttempt.builder()
                .assignmentStatus(as)
                .dateTimeStart(Instant.now())
                .uuid(UUID.randomUUID())
                .build();

        List<AssignmentFile> resources = getResourcesToCopy(state);
        List<AssignmentFile> assignmentFiles = getReadonlyAssignmentFilesToCompile(state);
        String assignment = competition.getCurrentAssignment().getAssignment().getName();

        // TODO this should be somewhere else
        Path teamAssignmentDir = teamService.getTeamAssignmentDirectory(competition.getCompetitionSession(), team, state
                .getAssignment());
        Path sourcesDir = teamAssignmentDir.resolve("sources");
        Path classesDir = teamAssignmentDir.resolve("classes");
        try {
            FileUtils.cleanDirectory(teamAssignmentDir.toFile());
            System.out.println("sources created? -> " + sourcesDir.toFile().mkdirs() + " as " + sourcesDir.toString());
            System.out.println("classes created? -> " + classesDir.toFile().mkdirs() + " as " + classesDir.toString());
        } catch (IOException e) {
            log.error("error while cleaning teamdir", e);
        }

        // copy resources
        resources.forEach(r -> {
            try {
                File target = classesDir.resolve(r.getFile()).toFile();
                if (target.getParentFile() != null) {
                    target.getParentFile().mkdirs();
                }
                FileUtils.copyFile(r.getAbsoluteFile().toFile(), target);
            } catch (IOException e) {
                log.error("error while writing resources to classes dir", e);
            }
        });

        // TODO fix compile result so team knows something is very wrong.
        try {
            message.getSources().forEach((uuid, v) -> {
                AssignmentFile orig = getOriginalAssignmentFile(uuid);
                File f = sourcesDir.resolve(orig.getFile()).toFile();
                try {
                    FileUtils.writeStringToFile(f, v, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.error("error while writing sourcefiles to sources dir", e);
                }
                assignmentFiles.add(orig.toBuilder()
                        .absoluteFile(f.toPath())
                        .build());
            });
        } catch (Exception e) {
            log.error("error while preparing sources.", e);
        }

        // C) Java compiler options
        try {
            boolean timedOut = false;
            int exitvalue = 0;
            final LengthLimitedOutputCatcher compileOutput = new LengthLimitedOutputCatcher(mojServerProperties.getLimits()
                    .getCompileOutputLimits());
            final LengthLimitedOutputCatcher compileErrorOutput = new LengthLimitedOutputCatcher(mojServerProperties.getLimits()
                    .getCompileOutputLimits());
            final Duration timeout = state.getAssignmentDescriptor()
                    .getCompileTimeout() != null ? state.getAssignmentDescriptor()
                    .getCompileTimeout() : mojServerProperties.getLimits().getCompileTimeout();
            try {
                List<String> cmd = new ArrayList<>();
                cmd.add(javaVersion.getCompiler().toString());
                cmd.add("-cp");
                cmd.add(makeClasspath(classesDir).stream()
                        .map(f -> f.getAbsoluteFile().toString())
                        .collect(Collectors.joining(File.pathSeparator)));
                cmd.add("-Xlint:all");
                cmd.add("-g:source,lines,vars");
                cmd.add("-d");
                cmd.add(classesDir.toAbsolutePath().toString());
                assignmentFiles.forEach(a -> {
                    cmd.add(a.getAbsoluteFile().toString());
                });

                final ProcessExecutor jUnitCommand = new ProcessExecutor().command(cmd);
                log.debug("Executing command {}", String.join(" \\\n", cmd));
                exitvalue = jUnitCommand.directory(teamAssignmentDir.toFile())
                        .timeout(timeout.toSeconds(), TimeUnit.SECONDS).redirectOutput(compileOutput)
                        .redirectError(compileErrorOutput).execute().getExitValue();
            } catch (TimeoutException e) {
                // process is automatically destroyed
                log.debug("Compile timed out and got killed for team {}.", team.getName());
                timedOut = true;
            } catch (SecurityException se) {
                log.error(se.getMessage(), se);
            }
            log.debug("exitValue {}", exitvalue);
            if (timedOut) {
                compileOutput.getBuffer()
                        .append('\n')
                        .append(mojServerProperties.getLimits().getCompileOutputLimits().getTimeoutMessage());
            }

            // TODO can this be done nicer?
            if (compileOutput.length() > 0) {
                // if we still have some output left and exitvalue = 0
                if (compileOutput.length() > 0 && exitvalue == 0 && !timedOut) {
                    compileAttempt = compileAttemptRepository.save(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .success(true)
                            .build());
                } else {
                    String output = stripTeamPathInfo(compileOutput.getBuffer(), FileUtils.getFile(teamAssignmentDir.toFile(), "sources"));
                    compileAttempt = compileAttemptRepository.save(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .success(false)
                            .compilerOutput(output)
                            .build());
                }
            } else {
                log.debug(compileOutput.toString());
                String output = stripTeamPathInfo(compileErrorOutput.getBuffer(), FileUtils.getFile(teamAssignmentDir.toFile(), "sources"));
                if ((exitvalue == 0) && !timedOut) {
                    compileAttempt = compileAttemptRepository.save(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .compilerOutput("OK")
                            .success(true)
                            .build());
                } else {
                    compileAttempt = compileAttemptRepository.save(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .success(false)
                            .compilerOutput(output)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return CompileResult.builder()
                .compileAttemptUuid(compileAttempt.getUuid())
                .dateTimeStart(compileAttempt.getDateTimeStart())
                .dateTimeEnd(compileAttempt.getDateTimeEnd())
                .compileOutput(compileAttempt.getCompilerOutput())
                .success(compileAttempt.isSuccess())
                .build();
    }

    private AssignmentFile getOriginalAssignmentFile(String uuid) {
        return competition.getActiveAssignment()
                .getAssignmentFiles()
                .stream()
                .filter(f -> f.getUuid().toString().equals(uuid))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find original assignment file for UUID " + uuid));
    }

    private String stripTeamPathInfo(StringBuilder result, File prefix) {
        if( result != null ) {
            return result.toString().replace(prefix.getAbsolutePath() + File.separator ,"");
        }
        return "";
    }

    private List<AssignmentFile> getReadonlyAssignmentFilesToCompile(ActiveAssignment state) {
        return state.getAssignmentFiles()
                .stream()
                .filter(f -> f.getFileType() == AssignmentFileType.READONLY ||
                        f.getFileType() == AssignmentFileType.TEST ||
                        f.getFileType() == AssignmentFileType.HIDDEN_TEST ||
                        f.getFileType() == AssignmentFileType.HIDDEN)
                .collect(Collectors.toList());
    }

    private List<AssignmentFile> getResourcesToCopy(ActiveAssignment state) {
        return state.getAssignmentFiles()
                .stream()
                .filter(f -> f.getFileType() == AssignmentFileType.RESOURCE ||
                        f.getFileType() == AssignmentFileType.TEST_RESOURCE ||
                        f.getFileType() == AssignmentFileType.HIDDEN_TEST_RESOURCE)
                .collect(Collectors.toList());
    }

    private List<File> makeClasspath(Path classesDir) {
        final List<File> classPath = new ArrayList<>();
        classPath.add(classesDir.toFile());
        classPath.add(
                FileUtils.getFile(mojServerProperties.getDirectories()
                        .getBaseDirectory()
                        .toFile(), mojServerProperties.getDirectories().getLibDirectory(), "junit-4.12.jar"));
        classPath.add(FileUtils.getFile(mojServerProperties.getDirectories()
                        .getBaseDirectory()
                        .toFile(), mojServerProperties.getDirectories().getLibDirectory(),
                "hamcrest-all-1.3.jar"));
        classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
                mojServerProperties.getDirectories().getLibDirectory(), "asciiart-core-1.1.0.jar"));
        for (File file : classPath) {
            if (!file.exists()) {
                log.error("not found: {}", file.getAbsolutePath());
            } else {
                log.trace("found: {}", file.getAbsolutePath());
            }
        }
        return classPath;
    }
}
