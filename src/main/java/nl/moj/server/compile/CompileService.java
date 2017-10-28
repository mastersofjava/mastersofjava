package nl.moj.server.compile;

import nl.moj.server.FeedbackController;
import nl.moj.server.SubmitController.SourceMessage;
import nl.moj.server.competition.Competition;
import nl.moj.server.files.AssignmentFile;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class CompileService {
    private final static String JAVA_SOURCE_EXTENSION = ".java";

    private static final Logger log = LoggerFactory.getLogger(CompileService.class);

	@Autowired
	private javax.tools.JavaCompiler javaCompiler;

	@Autowired
	private DiagnosticCollector<JavaFileObject> diagnosticCollector;

	@Autowired
	private FeedbackController feedbackController;

	@Autowired
	private Competition competition;

	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.libDirectory}")
	private String libDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;


	public Supplier<CompileResult> compile(SourceMessage message) {
		return compile(message, false, false);
	}

	public Supplier<CompileResult> compileForSubmit(SourceMessage message) {
		return compile(message, false, true);
	}

	public Supplier<CompileResult> compileWithTest(SourceMessage message) {
		return compile(message, true, false);
	}

	public Supplier<CompileResult> compile(SourceMessage message, boolean withTest, boolean forSubmit) {
		Supplier<CompileResult> supplier = () -> {
			Collection<AssignmentFile> assignmentFiles;
			if (withTest) {
				assignmentFiles = competition.getCurrentAssignment().getReadOnlyJavaAndTestFiles();
			} else {
				if (forSubmit) {
					assignmentFiles = competition.getCurrentAssignment().getReadOnlyJavaAndTestAndSubmitFiles();
				} else {
					assignmentFiles = competition.getCurrentAssignment().getReadOnlyJavaFiles();
				}
			}
			assignmentFiles.forEach(f -> log.debug(f.getName()));
			StandardJavaFileManager standardFileManager = javaCompiler.getStandardFileManager(diagnosticCollector, null,
					null);
			String assignment = competition.getCurrentAssignment().getName();
			File teamdir = FileUtils.getFile(basedir, teamDirectory, message.getTeam());

			List<JavaFileObject> javaFileObjects = assignmentFiles.stream().map(a -> {
				JavaFileObject jfo = createJavaFileObject(a.getFilename(), a.getContent());
				return jfo;
			}).collect(Collectors.toList());
			try {
				FileUtils.cleanDirectory(teamdir);
			} catch (IOException e) {
				log.error("error while cleaning teamdir",e);
			}
			message.getSource().forEach((k, v) -> {
				try {
					FileUtils.writeStringToFile(FileUtils.getFile(teamdir, "sources",assignment, k), v, Charset.defaultCharset());
				} catch (IOException e) {
					log.error("error while writing sourcefiles to teamdir",e);
				}
				javaFileObjects.add(createJavaFileObject(k, v));
			});
			// C) Java compiler options
			List<String> options = createCompilerOptions();


			PrintWriter err = new PrintWriter(System.err);
			log.info("compiling {} classes", javaFileObjects.size());
			List<File> files = new ArrayList<>();
			FileUtils.listFiles(teamdir, new String[] { "class" }, true).stream().forEach(f -> FileUtils.deleteQuietly(f));
			files.add(teamdir);
			// Create a compilation task.
			try {
				standardFileManager.setLocation(StandardLocation.CLASS_OUTPUT, files);
				standardFileManager.setLocation(StandardLocation.CLASS_PATH, makeClasspath(message.getTeam()));
			} catch (IOException e) {
				log.error(e.getMessage(),e);
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
				compileResult = new CompileResult(result, null, message.getTeam(), false, message.getScoreAtSubmissionTime());
				feedbackController.sendCompileFeedbackMessage(compileResult);
				return compileResult;
			}
			log.debug("compileSuccess: {}", true);
			compileResult = new CompileResult("Files compiled successfully.\n", message.getTests(), message.getTeam(), true, message.getScoreAtSubmissionTime());
			feedbackController.sendCompileFeedbackMessage(compileResult);
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
		classPath.add(FileUtils.getFile(basedir, teamDirectory, user));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "hamcrest-all-1.3.jar"));
		for (File file : classPath) {
			if (!file.exists()) {
				System.out.println("not found: " + file.getAbsolutePath());
			} else {
				System.out.println("found: " + file.getAbsolutePath());
			}
		}
		return classPath;
	}

	private String report(Diagnostic<?> dg) {
		StringBuilder sb = new StringBuilder();
		sb.append(dg.getKind() + "> Line=" + dg.getLineNumber() + ", Column=" + dg.getColumnNumber() + "\n");
		sb.append("Message> " + dg.getMessage(null) + "\n");
		//sb.append("Cause> " + dg.getCode() + "\n");
		//JavaFileObject jfo = (JavaFileObject) dg.getSource();
		//if (jfo != null) {
		//	sb.append(jfo.getName() + "\n");
		//}
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
