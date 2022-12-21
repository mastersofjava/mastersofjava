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
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.LengthLimitedOutputCatcher;

@Service
@Slf4j
public class CompileService {

	private CompileAttemptRepository compileAttemptRepository;
	private AssignmentStatusRepository assignmentStatusRepository;
	private MojServerProperties mojServerProperties;
	private TeamService teamService;
	private AssignmentService assignmentService;
	private static final boolean OS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

	public CompileService(MojServerProperties mojServerProperties, CompileAttemptRepository compileAttemptRepository,
			AssignmentStatusRepository assignmentStatusRepository, TeamService teamService,
			AssignmentService assignmentService) {
		this.mojServerProperties = mojServerProperties;
		this.compileAttemptRepository = compileAttemptRepository;
		this.assignmentStatusRepository = assignmentStatusRepository;
		this.teamService = teamService;
		this.assignmentService = assignmentService;
	}

	public CompletableFuture<CompileResult> scheduleCompile(CompileRequest compileRequest, Executor executor,
			ActiveAssignment activeAssignment) {
		// determine compiler version to use.
		final CompileInputWrapper input = new CompileInputWrapper(compileRequest, activeAssignment);

		log.info("supplyAsync.javaCompile {}", compileRequest.getSourceMessage().getAssignmentName());
		// compile code.

		log.info("javaCompile: {} for team {} ", input.getAssignmentName(),
				input.getTeamName());

		return CompletableFuture.supplyAsync(() -> javaCompile(input), executor).thenApply(output -> {
			CompileAttempt ca = createCompileAttempt(output);
			return createCompileResult(ca);

		});
	}

	private CompileAttempt createCompileAttempt(CompileOutputWrapper compileOutputWrapper) {

		log.debug("Creating compile attempt entity for assignment {}, competitionSession {} and team {}",
				compileOutputWrapper.getAssignmentId(), compileOutputWrapper.getCompetitionSessionId(),
				compileOutputWrapper.getTeamId());
		AssignmentStatus as = assignmentStatusRepository.findByAssignment_IdAndCompetitionSession_IdAndTeam_Id(
				compileOutputWrapper.getAssignmentId(), compileOutputWrapper.getCompetitionSessionId(),
				compileOutputWrapper.getTeamId());

		CompileAttempt compileAttempt = CompileAttempt.builder().assignmentStatus(as).dateTimeStart(Instant.now())
				.dateTimeEnd(compileOutputWrapper.getDateTimeEnd()).uuid(compileOutputWrapper.getCompileAttemptUuid())
				.build();
		compileAttemptRepository.save(compileAttempt);

		// TODO can this be done nicer?
		if (compileOutputWrapper.getOutput().length() > 0) {
			// if we still have some output left and exitvalue = 0
			if (compileOutputWrapper.getExitvalue() == 0 && !compileOutputWrapper.isTimedOut()) {
				compileAttempt = compileAttemptRepository.save(compileAttempt.toBuilder().success(true).build());
			} else {

				compileAttempt = compileAttemptRepository.save(compileAttempt.toBuilder().success(false)
						.compilerOutput(compileOutputWrapper.getOutput()).build());
			}
		} else {
			if ((compileOutputWrapper.getExitvalue() == 0) && !compileOutputWrapper.isTimedOut()) {
				// set some output since we are successful but the compiler had no output
				compileAttempt = compileAttemptRepository
						.save(compileAttempt.toBuilder().compilerOutput("OK").success(true).build());
			} else {
				compileAttempt = compileAttemptRepository.save(compileAttempt.toBuilder().success(false)
						.compilerOutput(compileOutputWrapper.getErrorOutput()).build());
			}
		}

		compileAttemptRepository.save(compileAttempt);

		return compileAttempt;

	}

	private CompileResult createCompileResult(CompileAttempt ca) {
		return CompileResult.builder().compileAttemptUuid(ca.getUuid()).dateTimeStart(ca.getDateTimeStart())
				.dateTimeEnd(ca.getDateTimeEnd()).compileOutput(ca.getCompilerOutput()).success(ca.isSuccess()).build();
	}

	private String toSafeFilePathInputForEachOperatingSystem(File file) {
		String safePathForEarchOperatingSystem = file.toString();
		if (safePathForEarchOperatingSystem.contains(" ") && OS_WINDOWS) {
			// if with space then make safe for javac execution (otherwise windows execution
			// would go wrong)
			safePathForEarchOperatingSystem = "\"" + safePathForEarchOperatingSystem + "\"";
		}
		return safePathForEarchOperatingSystem;
	}

	// TODO this will be client side
	private CompileOutputWrapper javaCompile(CompileInputWrapper input) {
		try {

			WorkspaceUtil workspace = new WorkspaceUtil(teamService, assignmentService, input);

			List<AssignmentFile> assignmentFiles = workspace.getReadonlyAssignmentFiles();
			log.info("resources: {}, assignmentFiles: {}", workspace.getResources().size(), assignmentFiles.size());
			workspace.cleanCompileLocationForTeam();
			// copy resources
			workspace.prepareResources(workspace.getResources());
			workspace.prepareInputSources(assignmentFiles, input.getSourceMessage(), input);

			// find java compiler
			AssignmentDescriptor ad = input.getAssignmentDescriptor();
			var javaVersion = mojServerProperties.getLanguages().getJavaVersion(ad.getJavaVersion());

			// C) Java compiler options
			boolean timedOut = false;
			int exitvalue = 0;
			final LengthLimitedOutputCatcher compileOutput = new LengthLimitedOutputCatcher(
					mojServerProperties.getLimits().getCompileOutputLimits());
			final LengthLimitedOutputCatcher compileErrorOutput = new LengthLimitedOutputCatcher(
					mojServerProperties.getLimits().getCompileOutputLimits());
			final Duration timeout = ad.getCompileTimeout() != null ? ad.getCompileTimeout()
					: mojServerProperties.getLimits().getCompileTimeout();

			final Instant dateTimeStart = Instant.now();

			try {
				List<String> cmd = new ArrayList<>();
				cmd.add(javaVersion.getCompiler().toString());
				cmd.add("-Xlint:all");

				boolean enablePreviewFeatures = ad.isJavaPreviewEnabled();
				if (javaVersion.getVersion() >= 11 && enablePreviewFeatures) {
					cmd.add("--enable-preview");
					cmd.add("--release");
					cmd.add("" + javaVersion.getVersion());
				}

				cmd.add("-encoding");
				cmd.add("UTF8");
				cmd.add("-g:source,lines,vars");
				cmd.add("-cp");
				cmd.add(makeClasspath(workspace.getClassesDir()).stream().map(f -> f.getAbsoluteFile().toString())
						.collect(Collectors.joining(File.pathSeparator)));
				cmd.add("-d");
				cmd.add(toSafeFilePathInputForEachOperatingSystem(workspace.getClassesDir().toAbsolutePath().toFile()));
				assignmentFiles.forEach(a -> {
					Assert.isTrue(a.getAbsoluteFile().toFile().exists(),
							"file does not exist: " + a.getAbsoluteFile().toFile());
					cmd.add(toSafeFilePathInputForEachOperatingSystem(a.getAbsoluteFile().toFile()));
				});

				long closeTimeout = timeout.toSeconds() + 4;

				final ProcessExecutor commandExecutor = new ProcessExecutor().command(cmd);
				commandExecutor.destroyOnExit().closeTimeout(closeTimeout, TimeUnit.SECONDS)
						.directory(workspace.getTeamAssignmentDir().toFile())
						.timeout(timeout.toSeconds(), TimeUnit.SECONDS).redirectOutput(compileOutput)
						.redirectError(compileErrorOutput);

				log.debug("Executing command {}", String.join(" \\\n", cmd));
				ProcessResult processResult = commandExecutor.execute();
				exitvalue = processResult.getExitValue();

				InputStream is = commandExecutor.pumps().getInput();
				OutputStream error = commandExecutor.pumps().getErr();
				OutputStream out = commandExecutor.pumps().getOut();
				commandExecutor.pumps().flush();
				if (is != null) {
					is.close();
				}
				if (error != null) {
					error.close();
				}
				if (out != null) {
					out.close();
				}
				log.debug("commandExecutor stop ");
				commandExecutor.pumps().stop();

			} catch (TimeoutException e) {
				// process is automatically destroyed
				log.debug("Compile timed out and got killed for team {}.", input.getTeamName());
				timedOut = true;
			} catch (SecurityException se) {
				log.error(se.getMessage(), se);
			}
			log.debug("exitValue {}, timeoutConfiguration {} ", exitvalue, timeout.toSeconds());
			if (timedOut) {
				compileOutput.getBuffer().append('\n')
						.append(mojServerProperties.getLimits().getCompileOutputLimits().getTimeoutMessage());
			}

			String output = stripTeamPathInfo(compileOutput.getBuffer(),
					FileUtils.getFile(workspace.getTeamAssignmentDir().toFile(), "sources"));

			String errorOutput = stripTeamPathInfo(compileErrorOutput.getBuffer(),
					FileUtils.getFile(workspace.getTeamAssignmentDir().toFile(), "sources"));

			var compileOutputWrapper = new CompileOutputWrapper(input, output, errorOutput, exitvalue, timedOut,
					dateTimeStart, Instant.now());

			return compileOutputWrapper;

		} catch (Exception e) {
			log.error("Unable to compile the assignment due to unexpected exception: {}", e.getMessage(), e);
			return new CompileOutputWrapper(input, "", "Unable to compile: " + e.getMessage(), -1, false, Instant.now(),
					Instant.now());
		}

	}

	private String stripTeamPathInfo(StringBuilder result, File prefix) {
		if (result != null) {
			return result.toString().replace(prefix.getAbsolutePath() + File.separator, "");
		}
		return "";
	}

	private List<File> makeClasspath(Path classesDir) {
		final List<File> classPath = new ArrayList<>();
		classPath.add(classesDir.toFile());
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
				mojServerProperties.getDirectories().getLibDirectory(), "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
				mojServerProperties.getDirectories().getLibDirectory(), "hamcrest-all-1.3.jar"));
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
