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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
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
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Service
@Slf4j
public class CompileService {

    private CompileAttemptRepository compileAttemptRepository;
    private AssignmentStatusRepository assignmentStatusRepository;
    private AssignmentRepository assignmentRepository;
    private AssignmentService assignmentService;

    private CompetitionRuntime competition;
    private MojServerProperties mojServerProperties;
    private TeamService teamService;

    public CompileService(CompetitionRuntime competition, MojServerProperties mojServerProperties,
                          CompileAttemptRepository compileAttemptRepository, AssignmentStatusRepository assignmentStatusRepository,
                          TeamService teamService,AssignmentService assignmentService, AssignmentRepository assignmentRepository) {
        this.competition = competition;
        this.mojServerProperties = mojServerProperties;
        this.compileAttemptRepository = compileAttemptRepository;
        this.assignmentStatusRepository = assignmentStatusRepository;
        this.teamService = teamService;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
    }

    public CompletableFuture<CompileResult> scheduleCompile(Team team, SourceMessage message, Executor executor) {
        // determine compiler version to use.
        ActiveAssignment state = competition.getActiveAssignment();
        Assert.isTrue(state!=null,"Active Assignment is missing.");
        AssignmentDescriptor input = state.getAssignmentDescriptor();
        final CompileInputWrapper compileInputWrapper;
        if (input==null && team.getRole().equals(Role.ADMIN)) {
            log.info("team " + team.getName() + " " + team.getRole() );
            log.info("input.sources " + message.getSources().size() + " " + message.getTests().size() + " " + message.getSources().keySet() + " " +message.getAssignmentName());
            compileInputWrapper = new CompileInputWrapper(message.getAssignmentName());
            input = compileInputWrapper.assignmentDescriptor;
        } else {
            compileInputWrapper = new CompileInputWrapper(state);
        }
        Assert.isTrue(input!=null,"assignment descriptor is missing.");
        var javaVersion = mojServerProperties.getLanguages()
                .getJavaVersion(input
                        .getJavaVersion());

       // TeamProjectPathModel pathModel = new TeamProjectPathModel(team, compileInputWrapper.assignment);
       // boolean isCleaned = pathModel.cleanCompileLocationForTeam();

        log.info("supplyAsync.javaCompile " + message.getAssignmentName() + " " +javaVersion  );
        // compile code.
        return CompletableFuture.supplyAsync(() -> javaCompile(javaVersion, team, message, compileInputWrapper), executor);
    }

    public class CompileInputWrapper {
        List<AssignmentFile> resources;
        List<AssignmentFile> readonlyAssignmentFiles;
        List<AssignmentFile> allAssignmentFiles;
        Assignment assignment;
        AssignmentDescriptor assignmentDescriptor;
        Instant startTimeSinceQueue;
        private UUID compileAttemptId;
        CompileInputWrapper(String assignmentName) {
            assignment = assignmentRepository.findByName(assignmentName);
            assignmentDescriptor = assignmentService.getAssignmentDescriptor(assignment);
            List<AssignmentFile> fileList = assignmentService.getAssignmentFiles(assignment);
            resources = getResourcesToCopy(fileList);
            readonlyAssignmentFiles = getReadonlyAssignmentFilesToCompile(fileList);
            allAssignmentFiles = fileList;
        }
        CompileInputWrapper(ActiveAssignment state) {
            //AssignmentFileType: RESOURCE, TEST_RESOURCE, HIDDEN_TEST_RESOURCE
            resources = getResourcesToCopy(state);
            //AssignmentFileType: READONLY, TEST, HIDDEN_TEST, HIDDEN
            readonlyAssignmentFiles = getReadonlyAssignmentFilesToCompile(state);
            assignment = state.getAssignment();
            assignmentDescriptor = state.getAssignmentDescriptor();
            allAssignmentFiles = state.getAssignmentFiles();
        }
        public void destroy() {
            assignment = null;
            assignmentDescriptor = null;
            resources = null;
            readonlyAssignmentFiles = null;
            allAssignmentFiles=null;
        }
        private List<AssignmentFile> getReadonlyAssignmentFilesToCompile(ActiveAssignment state) {
            return getReadonlyAssignmentFilesToCompile(state.getAssignmentFiles());
        }
        /**
         * AssignmentFileType: READONLY, TEST, HIDDEN_TEST, HIDDEN
         */
        private List<AssignmentFile> getReadonlyAssignmentFilesToCompile(List<AssignmentFile> fileList) {
            return fileList
                    .stream()
                    .filter(f -> f.getFileType() == AssignmentFileType.READONLY ||
                            f.getFileType() == AssignmentFileType.TEST ||
                            f.getFileType() == AssignmentFileType.HIDDEN_TEST ||
                            f.getFileType() == AssignmentFileType.HIDDEN)
                    .collect(Collectors.toList());
        }
        private List<AssignmentFile> getResourcesToCopy(ActiveAssignment state) {
            return getResourcesToCopy(state.getAssignmentFiles());
        }
        /**
         * AssignmentFileType: RESOURCE, TEST_RESOURCE, HIDDEN_TEST_RESOURCE
         */
        private List<AssignmentFile> getResourcesToCopy(List<AssignmentFile> fileList) {
            return fileList
                    .stream()
                    .filter(f -> f.getFileType() == AssignmentFileType.RESOURCE ||
                            f.getFileType() == AssignmentFileType.TEST_RESOURCE ||
                            f.getFileType() == AssignmentFileType.HIDDEN_TEST_RESOURCE)
                    .collect(Collectors.toList());
        }
        private AssignmentFile getOriginalAssignmentFile(String uuid) {
            return allAssignmentFiles
                    .stream()
                    .filter(f -> f.getUuid().toString().equals(uuid))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find original assignment file for UUID " + uuid));
        }
    }
    public class TeamProjectPathModel {
        private Path teamAssignmentDir;
        private Path sourcesDir;
        private Path classesDir;
        private String errorMessage;

        public TeamProjectPathModel(Team team, Assignment assignment) {
            teamAssignmentDir = teamService.getTeamAssignmentDirectory(competition.getCompetitionSession(), team, assignment);
            sourcesDir = teamAssignmentDir.resolve("sources");
            classesDir = teamAssignmentDir.resolve("classes");
        }

        public boolean cleanCompileLocationForTeam() {
            boolean isValidCleanStart = false;
            try {
                if (teamAssignmentDir.toFile().exists()) {

                    Collection<File> fileList = FileUtils.listFiles(teamAssignmentDir.toFile(), new String[] {"java","class"}, true);

                    for (File file: fileList) {
                        if (file.exists()) {
                            FileUtils.deleteQuietly(file);
                            File project = file.getParentFile().getParentFile();
                            FileUtils.deleteQuietly(project);
                        }
                    }
                    isValidCleanStart = !teamAssignmentDir.toFile().exists() || teamAssignmentDir.toFile().list().length==0;
                } else {
                    isValidCleanStart = true;
                }



            } catch (Exception e) {
                log.error("error while cleaning teamdir: " +teamAssignmentDir.toFile(), e);
            }
            boolean isValidSources= sourcesDir.toFile().mkdirs();
            boolean isValidClasses= classesDir.toFile().mkdirs();
            isValidCleanStart &= isValidSources &&  isValidClasses;
            log.info("cleanedDirectory: " + teamAssignmentDir + " " +isValidCleanStart);
            System.out.println("sources created? -> " + isValidSources + " as " + sourcesDir.toString());
            System.out.println("classes created? -> " + isValidClasses + " as " + classesDir.toString());
            return isValidCleanStart;
        }
        public void destroy() {
            sourcesDir = null;
            classesDir = null;
            teamAssignmentDir = null;
        }
    }

    private String toSafeClasspathInputForEachOperatingSystem(File file) {
        String safePathForEarchOperatingSystem = file.toString();
        if (safePathForEarchOperatingSystem.contains(" ")) {
            // if with space then make safe for javac execution (otherwise windows execution would go wrong)
            safePathForEarchOperatingSystem = "\"" +safePathForEarchOperatingSystem + "\"";
        }
        return safePathForEarchOperatingSystem;
    }
    public TeamProjectPathModel createTeamProjectPathModel(Team team, Assignment assignment) {
        return new TeamProjectPathModel(team, assignment);
    }

    private CompileResult javaCompile(Languages.JavaVersion javaVersion, Team team, SourceMessage message, CompileInputWrapper compileInputWrapper) {
        compileInputWrapper.startTimeSinceQueue = Instant.now();
        // TODO should not be here.
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(compileInputWrapper.assignment, competition
                .getCompetitionSession(), team);
        log.info("javaCompile: " + compileInputWrapper.assignment.getName() + " for team " +team.getName());
        compileInputWrapper.compileAttemptId = UUID.randomUUID();
        CompileAttempt compileAttempt = CompileAttempt.builder()
                .assignmentStatus(as)
                .dateTimeStart(Instant.now())
                .uuid(compileInputWrapper.compileAttemptId)
                .build();
        List<AssignmentFile> resources = compileInputWrapper.resources;
        List<AssignmentFile> assignmentFiles = compileInputWrapper.readonlyAssignmentFiles;
        log.info("resources: " + resources.size() + " assignmentFiles: " + assignmentFiles.size());

        // TODO this should be somewhere else
        TeamProjectPathModel pathModel = createTeamProjectPathModel(team, compileInputWrapper.assignment);
        pathModel.cleanCompileLocationForTeam();
        // copy resources
        resources.forEach(r -> {
            try {
                File target = pathModel.classesDir.resolve(r.getFile()).toFile();
                File parentFile = target.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                    target.getParentFile().mkdirs();
                }
                FileUtils.copyFile(r.getAbsoluteFile().toFile(), target);
            } catch (IOException e) {
                log.error("error while writing resources to classes dir", e);
                pathModel.errorMessage =  e.getMessage();
            }
        });
        // TODO fix compile result so team knows something is very wrong.
        try {
            message.getSources().forEach((uuid, v) -> {
                try {
                    AssignmentFile orig = compileInputWrapper.getOriginalAssignmentFile(uuid);
                    File f = pathModel.sourcesDir.resolve(orig.getFile()).toFile();
                    File parentFile = f.getParentFile();
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    Files.deleteIfExists(f.toPath());
                    FileUtils.writeStringToFile(f, v, StandardCharsets.UTF_8);
                    assignmentFiles.add(orig.toBuilder()
                            .absoluteFile(f.toPath())
                            .build());
                } catch (IOException|RuntimeException e) {
                    log.error("error while writing sourcefiles to sources dir", e);
                    pathModel.errorMessage = e.getMessage();
                }

            });
            Assert.isTrue(pathModel.errorMessage==null,pathModel.errorMessage);
        } catch (Exception e) {
            log.error("error while preparing sources.", e);
            return createCompileResult(compileInputWrapper, "error while preparing sources: "+pathModel.errorMessage, false);
        }

        // C) Java compiler options
        try {
            boolean timedOut = false;
            int exitvalue = 0;
            final LengthLimitedOutputCatcher compileOutput = new LengthLimitedOutputCatcher(mojServerProperties.getLimits()
                    .getCompileOutputLimits());
            final LengthLimitedOutputCatcher compileErrorOutput = new LengthLimitedOutputCatcher(mojServerProperties.getLimits()
                    .getCompileOutputLimits());
            final Duration timeout = compileInputWrapper.assignmentDescriptor.getCompileTimeout() != null ? compileInputWrapper.assignmentDescriptor
                    .getCompileTimeout() : mojServerProperties.getLimits().getCompileTimeout();
            try {
                List<String> cmd = new ArrayList<>();
                cmd.add(javaVersion.getCompiler().toString());
                cmd.add("-Xlint:all");
                if (javaVersion.getVersion()>12) {
                    cmd.add("--enable-preview");
                    cmd.add("--release");
                    cmd.add("" +javaVersion.getVersion());
                }

                cmd.add("-encoding");
                cmd.add("UTF8");
                cmd.add("-g:source,lines,vars");
                cmd.add("-cp");
                cmd.add(makeClasspath(pathModel.classesDir).stream()
                        .map(f -> f.getAbsoluteFile().toString())
                        .collect(Collectors.joining(File.pathSeparator)));
                cmd.add("-d");
                cmd.add(toSafeClasspathInputForEachOperatingSystem(pathModel.classesDir.toAbsolutePath().toFile()));
                assignmentFiles.forEach(a -> {
                    Assert.isTrue(a.getAbsoluteFile().toFile().exists(),"file does not exist: " +a.getAbsoluteFile().toFile());
                    cmd.add(toSafeClasspathInputForEachOperatingSystem(a.getAbsoluteFile().toFile()));
                });

                long closeTimeout = timeout.toSeconds() + 4;

                final ProcessExecutor commandExecutor = new ProcessExecutor().command(cmd);
                commandExecutor.destroyOnExit().closeTimeout(closeTimeout, TimeUnit.SECONDS).directory(pathModel.teamAssignmentDir.toFile())
                        .timeout(timeout.toSeconds(), TimeUnit.SECONDS).redirectOutput(compileOutput)
                        .redirectError(compileErrorOutput);

                log.debug("Executing command {}", String.join(" \\\n", cmd));
                ProcessResult processResult = commandExecutor.execute();
                exitvalue = processResult.getExitValue();

                InputStream is = commandExecutor.pumps().getInput();
                OutputStream error = commandExecutor.pumps().getErr();
                OutputStream out = commandExecutor.pumps().getOut();
                log.debug("close " +(is!=null) + "-"+(error!=null)+"-"+(out!=null) );
                commandExecutor.pumps().flush();
                if (is!=null) {
                    is.close();
                }
                if (error!=null) {
                    error.close();
                }
                if (out!=null) {
                    out.close();
                }
                log.debug("commandExecutor stop ");
                commandExecutor.pumps().stop();

            } catch (TimeoutException e) {
                // process is automatically destroyed
                log.debug("Compile timed out and got killed for team {}.", team.getName());
                timedOut = true;
            } catch (SecurityException se) {
                log.error(se.getMessage(), se);
            }
            log.debug("exitValue {}, timeoutConfiguration {} ", exitvalue, timeout.toSeconds());
            if (timedOut) {
                compileOutput.getBuffer()
                        .append('\n')
                        .append(mojServerProperties.getLimits().getCompileOutputLimits().getTimeoutMessage());
            }
            boolean isAlwaysNeeded = !team.getRole().equals(Role.ADMIN); // if admin then no registration is needed, when not in competition (only validating the assignment).
            // TODO can this be done nicer?
            if (compileOutput.length() > 0) {
                // if we still have some output left and exitvalue = 0
                if (compileOutput.length() > 0 && exitvalue == 0 && !timedOut) {
                    compileAttempt = registerWhenNeeded(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .success(true)
                            .build(), isAlwaysNeeded);
                } else {
                    String output = stripTeamPathInfo(compileOutput.getBuffer(), FileUtils.getFile(pathModel.teamAssignmentDir.toFile(), "sources"));
                    compileAttempt = registerWhenNeeded(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .success(false)
                            .compilerOutput(output)
                            .build(), isAlwaysNeeded);
                }
            } else {
                log.debug(compileOutput.toString());
                String output = stripTeamPathInfo(compileErrorOutput.getBuffer(), FileUtils.getFile(pathModel.teamAssignmentDir.toFile(), "sources"));
                if ((exitvalue == 0) && !timedOut) {
                    compileAttempt = registerWhenNeeded(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .compilerOutput("OK")
                            .success(true)
                            .build(), isAlwaysNeeded);
                } else {
                    compileAttempt = registerWhenNeeded(compileAttempt.toBuilder()
                            .dateTimeEnd(Instant.now())
                            .success(false)
                            .compilerOutput(output)
                            .build(), isAlwaysNeeded);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return createCompileResult(compileInputWrapper, compileAttempt.getCompilerOutput(), compileAttempt.isSuccess());
    }
    private CompileResult createCompileResult(CompileInputWrapper compileInputWrapper, String outputMessage, boolean isSuccess) {
        return CompileResult.builder()
                .compileAttemptUuid(compileInputWrapper.compileAttemptId)
                .dateTimeStart(compileInputWrapper.startTimeSinceQueue)
                .dateTimeEnd(Instant.now())
                .compileOutput(outputMessage)
                .success(isSuccess)
                .build();
    }
    private CompileAttempt registerWhenNeeded(CompileAttempt input, boolean isAlwaysNeeded) {
        CompileAttempt result = input;
        if (input.getAssignmentStatus()!=null||isAlwaysNeeded) {
            result = compileAttemptRepository.save(input);
        }
        return result;
    }


    private String stripTeamPathInfo(StringBuilder result, File prefix) {
        if( result != null ) {
            return result.toString().replace(prefix.getAbsolutePath() + File.separator ,"");
        }
        return "";
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
