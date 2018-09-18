package nl.moj.server.compiler;

import lombok.AllArgsConstructor;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.SubmitController.SourceMessage;
import nl.moj.server.config.properties.Languages;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompileService {
	private final static String JAVA_SOURCE_EXTENSION = ".java";

	private static final Logger log = LoggerFactory.getLogger(CompileService.class);

	private javax.tools.JavaCompiler javaCompiler;

	private DiagnosticCollector<JavaFileObject> diagnosticCollector;

	private FeedbackMessageController feedbackMessageController;

	private CompetitionRuntime competition;

	private MojServerProperties mojServerProperties;

	private TeamRepository teamRepository;

	private static JavaFileObject createJavaFileObject(String className, String sourceCode) {
		return new SourceJavaFileObject(className, sourceCode);
	}

	private static URI toURI(String name) {
		File file = new File(name);
		if (file.exists()) {
			return file.toURI();
		} else {
			try {
				final StringBuilder newUri = new StringBuilder();
				newUri.append("mfm:///");
				newUri.append(name.replace('.', '/'));

				if (name.endsWith(JAVA_SOURCE_EXTENSION)) {
					newUri.replace(newUri.length() - JAVA_SOURCE_EXTENSION.length(), newUri.length(),
							JAVA_SOURCE_EXTENSION);
				}

				return URI.create(newUri.toString());
			} catch (Exception exp) {
				return URI.create("mfm:///org/patrodyne/scripting/java/java_source");
			}
		}
	}

	public Supplier<CompileResult> compile(SourceMessage message) {
		return compile(message, false, false);
	}

	public Supplier<CompileResult> compileForSubmit(SourceMessage message) {
		return compile(message, false, true);
	}

	public Supplier<CompileResult> compileWithTest(SourceMessage message) {
		log.debug("compileWithTest");
		return compile(message, true, false);
	}

	private Supplier<CompileResult> compile(SourceMessage message, boolean withTest, boolean forSubmit) {
		return compileNew(mojServerProperties.getLanguages().getJavaVersion(),message,withTest,forSubmit);
	}

	private Supplier<CompileResult> compileNew(Languages.JavaVersion javaVersion, SourceMessage message, boolean withTest, boolean forSubmit) {
		return () -> {
			List<AssignmentFile> assignmentFiles = createAssignmentFiles(withTest, forSubmit);
			String assignment = competition.getCurrentAssignment().getAssignment().getName();
			File teamdir = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getTeamDirectory(), message.getTeam());

			try {
				FileUtils.cleanDirectory(teamdir);
			} catch (IOException e) {
				log.error("error while cleaning teamdir", e);
			}
			message.getSource().forEach((k, v) -> {
				File f = FileUtils.getFile(teamdir, "sources", assignment, k);
				try {
					FileUtils.writeStringToFile(f, v, StandardCharsets.UTF_8);
				} catch (IOException e) {
					log.error("error while writing sourcefiles to teamdir", e);
				}
				assignmentFiles.add(AssignmentFile.builder()
						.content(v)
						.file(f.toPath())
						.filename(f.getName())
						.name(f.getName())
						.fileType(AssignmentFileType.EDIT)
						.assignment(assignment)
						.readOnly(false)
						.build());
			});
			// C) Java compiler options
			CompileResult compileResult = null;
			try {
				boolean isRunTerminated = false;
				int exitvalue = 0;
				final LengthLimitedOutputCatcher compileOutput = new LengthLimitedOutputCatcher(mojServerProperties);
				final LengthLimitedOutputCatcher compileErrorOutput = new LengthLimitedOutputCatcher(mojServerProperties);
				try {
					List<String> cmd = new ArrayList<>();
					cmd.add(javaVersion.getCompiler().toString());
					cmd.add("-cp");
					cmd.add(makeClasspath(message.getTeam()).stream().map( f -> f.getAbsoluteFile().toString()).collect(Collectors.joining(File.pathSeparator)));
					cmd.add("-Xlint:all");
					cmd.add("-g:source,lines,vars");
					cmd.add("-d");
					cmd.add(teamdir.getAbsoluteFile().toString());
					assignmentFiles.forEach( a -> {
						cmd.add(a.getFile().toAbsolutePath().toString());
					});

					final ProcessExecutor jUnitCommand = new ProcessExecutor().command(cmd);
					log.debug("Executing command {}", String.join(" \\\n", cmd));
					exitvalue = jUnitCommand.directory(teamdir)
							.timeout(mojServerProperties.getLimits().getUnitTestTimeoutSeconds(), TimeUnit.SECONDS).redirectOutput(compileOutput)
							.redirectError(compileErrorOutput).execute().getExitValue();
				} catch (TimeoutException e) {
					// process is automatically destroyed
					log.debug("Compile timed out and got killed", message.getTeam());
					isRunTerminated = true;
				} catch (SecurityException se) {
					log.error(se.getMessage(), se);
				}
				log.debug("exitValue {}", exitvalue);
				if (isRunTerminated) {
					compileOutput.getBuffer().append('\n').append(mojServerProperties.getLimits().getUnitTestOutput().getTestTimeoutTermination());
				}

				final boolean success;
				final String result;
				if (compileOutput.length() > 0) {
					// if we still have some output left and exitvalue = 0
					if (compileOutput.length() > 0 && exitvalue == 0 && !isRunTerminated) {
						success = true;
						compileResult = new CompileResult("Files compiled successfully.\n", message.getTests(),
								message.getTeam(), true, message.getScoreAtSubmissionTime());
					} else {
						success = false;
						stripTeamPathInfo(compileOutput.getBuffer(), FileUtils.getFile(teamdir, "sources", assignment));
						compileResult = new CompileResult(compileOutput.toString(), message.getTests(),
								message.getTeam(), false, message.getScoreAtSubmissionTime());
					}
				} else {
					log.debug(compileOutput.toString());
					stripTeamPathInfo(compileErrorOutput.getBuffer(), FileUtils.getFile(teamdir, "sources", assignment));
					result = compileErrorOutput.toString();
					success = (exitvalue == 0) && !isRunTerminated;
					compileResult = new CompileResult(success?"Files compiled successfully.\n":result, message.getTests(),
							message.getTeam(), success, message.getScoreAtSubmissionTime());
				}

				log.debug("compile success {}", success);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			feedbackMessageController.sendCompileFeedbackMessage(compileResult);
			return compileResult;
		};
	}

	private void stripTeamPathInfo(StringBuilder result, File prefix ) {
		final Matcher matcher = Pattern.compile("^("+prefix.getAbsolutePath()+").*").matcher(result);
		if (matcher.find()) {
			log.trace("stripped '{}'", matcher.group());
			result.delete(0, matcher.end());
			if (result.length() > 0 && result.charAt(0) == '\n') {
				result.deleteCharAt(0);
			}
		}
	}

	private Supplier<CompileResult> compileOld(SourceMessage message, boolean withTest, boolean forSubmit) {
		Supplier<CompileResult> supplier = () -> {
			Team team = teamRepository.findByName(message.getTeam());
			List<AssignmentFile> assignmentFiles = createAssignmentFiles(withTest, forSubmit);
			StandardJavaFileManager standardFileManager = javaCompiler.getStandardFileManager(diagnosticCollector, null,
					null);
			String assignment = competition.getCurrentAssignment().getAssignment().getName();
			File teamdir = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getTeamDirectory(), message.getTeam());

			List<JavaFileObject> javaFileObjects = assignmentFiles.stream().map(a -> {
				JavaFileObject jfo = createJavaFileObject(a.getFilename(), a.getContent());
				return jfo;
			}).collect(Collectors.toList());
			try {
				FileUtils.cleanDirectory(teamdir);
			} catch (IOException e) {
				log.error("error while cleaning teamdir", e);
			}
			message.getSource().forEach((k, v) -> {
				try {
					FileUtils.writeStringToFile(FileUtils.getFile(teamdir, "sources", assignment, k), v,
							Charset.defaultCharset());
				} catch (IOException e) {
					log.error("error while writing sourcefiles to teamdir", e);
				}
				javaFileObjects.add(createJavaFileObject(k, v));
			});
			// C) Java compiler options
			List<String> options = createCompilerOptions();

			PrintWriter err = new PrintWriter(System.err);
			log.info("compiling {} classes", javaFileObjects.size());
			List<File> files = new ArrayList<>();
			FileUtils.listFiles(teamdir, new String[]{"class"}, true).stream()
					.forEach(f -> FileUtils.deleteQuietly(f));
			files.add(teamdir);
			// Create a compilation task.
			try {
				standardFileManager.setLocation(StandardLocation.CLASS_OUTPUT, files);
				standardFileManager.setLocation(StandardLocation.CLASS_PATH, makeClasspath(message.getTeam()));
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
			CompilationTask compilationTask = javaCompiler.getTask(err, standardFileManager, diagnosticCollector,
					options, null, javaFileObjects);
			CompileResult compileResult;
			if (!compilationTask.call()) {
				StringBuilder sb = new StringBuilder();
				for (Diagnostic<?> diagnostic : diagnosticCollector.getDiagnostics()) {
					sb.append(report(diagnostic));
				}
				String result = sb.toString();
				diagnosticCollector = new DiagnosticCollector<>();
				log.debug("compileSuccess: {}\n{}", false, result);
				List<String> tests = new ArrayList<>();
				compileResult = new CompileResult(result, tests, message.getTeam(), false,
						message.getScoreAtSubmissionTime());
			} else {
				log.debug("compileSuccess: {}", true);
				compileResult = new CompileResult("Files compiled successfully.\n", message.getTests(),
						message.getTeam(), true, message.getScoreAtSubmissionTime());
			}
			feedbackMessageController.sendCompileFeedbackMessage(compileResult);
			return compileResult;
		};
		return supplier;
	}

	private List<AssignmentFile> createAssignmentFiles(boolean withTest, boolean forSubmit) {
		List<AssignmentFile> assignmentFiles;
		AssignmentState state = competition.getAssignmentState();
		if (withTest) {
			assignmentFiles = state.getAssignmentFiles()
					.stream()
					.filter(f -> f.getFileType() == AssignmentFileType.READONLY ||
							f.getFileType() == AssignmentFileType.TEST)
					.collect(Collectors.toList());
		} else {
			if (forSubmit) {
				assignmentFiles = state.getAssignmentFiles()
						.stream()
						.filter(f -> f.getFileType() == AssignmentFileType.READONLY ||
								f.getFileType() == AssignmentFileType.TEST ||
								f.getFileType() == AssignmentFileType.SUBMIT)
						.collect(Collectors.toList());
			} else {
				assignmentFiles = state.getAssignmentFiles()
						.stream()
						.filter(f -> f.getFileType() == AssignmentFileType.READONLY)
						.collect(Collectors.toList());
			}
		}
		assignmentFiles.forEach(f -> log.trace(f.getName()));
		return assignmentFiles;
	}

	private List<String> createCompilerOptions() {
		List<String> options = new ArrayList<>();
		// enable all recommended warnings.
		options.add("-Xlint:all");
		// enable debugging for line numbers and local variables.
		options.add("-g:source,lines,vars");

		return options;
	}

	private List<File> makeClasspath(String user) {
		final List<File> classPath = new ArrayList<>();
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getTeamDirectory(), user));
		classPath.add(
				FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getLibDirectory(), "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getLibDirectory(),
				"hamcrest-all-1.3.jar"));
		for (File file : classPath) {
			if (!file.exists()) {
				log.error("not found: {}", file.getAbsolutePath());
			} else {
				log.trace("found: {}", file.getAbsolutePath());
			}
		}
		return classPath;
	}

	private String report(Diagnostic<?> dg) {
		StringBuilder sb = new StringBuilder();
		sb.append(dg.getKind() + "> Line=" + dg.getLineNumber() + ", Column=" + dg.getColumnNumber() + "\n");
		sb.append("Message> " + dg.getMessage(null) + "\n");
		return sb.toString();
	}

	/**
	 * A subclass of JavaFileObject used to represent Java source coming from a
	 * string.
	 * <p>
	 * Removed unused method: public Reader openReader().
	 */
	private static class SourceJavaFileObject extends SimpleJavaFileObject {
		private final String sourceCode;

		protected SourceJavaFileObject(String name, String sourceCode) {
			super(toURI(name), Kind.SOURCE);
			this.sourceCode = sourceCode;
		}

		@Override
		public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
			return CharBuffer.wrap(sourceCode);
		}
	}
}
