package nl.moj.server.compile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.moj.server.SubmitController.SourceMessage;
import nl.moj.server.competition.Competition;
import nl.moj.server.files.AssignmentFile;

@Service
public class CompileService {

	private static final Logger log = LoggerFactory.getLogger(CompileService.class);

	@Autowired
	private javax.tools.JavaCompiler javaCompiler;
	@Autowired
	private DiagnosticCollector<JavaFileObject> diagnosticCollector;
	// @Autowired
	// private MemoryJavaFileManager<StandardJavaFileManager> javaFileManager;

	// @Autowired
	// private StandardJavaFileManager standardFileManager;

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
				assignmentFiles.forEach(f -> log.debug(f.getName()));
			} else {
				if (forSubmit) {
					assignmentFiles = competition.getCurrentAssignment().getReadOnlyJavaAndSubmitFiles();
				} else {
					assignmentFiles = competition.getCurrentAssignment().getReadOnlyJavaFiles();
				}
			}

			List<JavaFileObject> javaFileObjects = assignmentFiles.stream().map(a -> {
				JavaFileObject jfo = MemoryJavaFileManager.createJavaFileObject(a.getFilename(), a.getContent());
				return jfo;
			}).collect(Collectors.toList());

			message.getSource()
					.forEach((k, v) -> javaFileObjects.add(MemoryJavaFileManager.createJavaFileObject(k, v)));

			// C) Java compiler options
			List<String> options = createCompilerOptions();

			PrintWriter err = new PrintWriter(System.err);
			log.info("compiling {} classes", javaFileObjects.size());
			List<File> files = new ArrayList<>();
			File file = FileUtils.getFile(basedir, teamDirectory, message.getTeam());
			FileUtils.listFiles(file, new String[] { "class" }, true).stream().forEach(f -> FileUtils.deleteQuietly(f));
			files.add(file);
			StandardJavaFileManager standardFileManager = javaCompiler.getStandardFileManager(diagnosticCollector, null,
					null);
			// Create a compilation task.
			try {
				standardFileManager.setLocation(StandardLocation.CLASS_OUTPUT, files);
				standardFileManager.setLocation(StandardLocation.CLASS_PATH, makeClasspath(message.getTeam()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			CompilationTask compilationTask = javaCompiler.getTask(err, standardFileManager, diagnosticCollector,
					options, null, javaFileObjects);

			String result = "Success\n";
			if (!compilationTask.call()) {
				StringBuilder sb = new StringBuilder();
				for (Diagnostic<?> diagnostic : diagnosticCollector.getDiagnostics()) {
                    sb.append(report(diagnostic));
                }
				result = sb.toString();
				diagnosticCollector = new DiagnosticCollector<>();
				log.debug("compileSuccess: {}\n{}", false, result);
				// standardFileManager.
				return new CompileResult(result, null, message.getTeam(), false);
			}
			log.debug("compileSuccess: {}", true);
			return new CompileResult(result, message.getTests(), message.getTeam(), true);
		};
		return supplier;
	}

	private List<String> createCompilerOptions() {
		List<String> options = new ArrayList<>();
		// enable all recommended warnings.
		options.add("-Xlint:all");
		// enable debugging for line numbers and local variables.
		options.add("-g:lines,vars");

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
		sb.append("Cause> " + dg.getCode() + "\n");
		JavaFileObject jfo = (JavaFileObject) dg.getSource();
		if (jfo != null) {
			sb.append(jfo.getName() + "\n");
		}
		return sb.toString();
	}
}
