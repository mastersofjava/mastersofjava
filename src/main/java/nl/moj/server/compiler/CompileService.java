package nl.moj.server.compiler;

import lombok.AllArgsConstructor;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.SubmitController.SourceMessage;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
		Supplier<CompileResult> supplier = () -> {
			Team team = teamRepository.findByName(message.getTeam());
			List<AssignmentFile> assignmentFiles;
			AssignmentState state = competition.getAssignmentState();
			if (withTest) {
				assignmentFiles = state.getAssignmentFiles()
						.stream()
						.filter( f -> f.getFileType() == AssignmentFileType.READONLY ||
								f.getFileType() == AssignmentFileType.TEST )
						.collect(Collectors.toList());
			} else {
				if (forSubmit) {
					assignmentFiles = state.getAssignmentFiles()
							.stream()
							.filter( f -> f.getFileType() == AssignmentFileType.READONLY ||
									f.getFileType() == AssignmentFileType.TEST ||
									f.getFileType() == AssignmentFileType.SUBMIT )
							.collect(Collectors.toList());
				} else {
					assignmentFiles = state.getAssignmentFiles()
							.stream()
							.filter( f -> f.getFileType() == AssignmentFileType.READONLY)
							.collect(Collectors.toList());
				}
			}
			assignmentFiles.forEach(f -> log.trace(f.getName()));
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
			FileUtils.listFiles(teamdir, new String[] { "class" }, true).stream()
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

	/**
	 * A subclass of JavaFileObject used to represent Java source coming from a
	 * string.
	 *
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
