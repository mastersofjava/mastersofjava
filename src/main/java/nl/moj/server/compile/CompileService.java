package nl.moj.server.compile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.AssignmentService;
import nl.moj.server.files.AssignmentFile;

@Service
public class CompileService {

	
	private static final Logger log = LoggerFactory.getLogger(CompileService.class);

	@Autowired
	private javax.tools.JavaCompiler javaCompiler;
	@Autowired
	private DiagnosticCollector<JavaFileObject> diagnosticCollector;
	@Autowired
	private MemoryJavaFileManager<StandardJavaFileManager> javaFileManager;

	@Autowired
	private AssignmentService assignmentService;

	public Supplier<CompileResult> compile(Map<String, String> sources, String user) {
		return compile(sources, user, false);
	}
	
	public Supplier<CompileResult> compile(Map<String, String> sources, String user, boolean withTest) {
		Supplier<CompileResult> supplier = () -> {
			Collection<AssignmentFile> assignmentFiles;
			if (withTest) {
				assignmentFiles = assignmentService.getReadOnlyJavaAndTestFiles();
			} else {
				assignmentFiles = assignmentService.getReadOnlyJavaFiles();
			}
			
			List<JavaFileObject> javaFileObjects = assignmentFiles.stream()
					.map(a -> {
						JavaFileObject jfo = MemoryJavaFileManager.createJavaFileObject(a.getFilename(),
								a.getContent());
						return jfo;
					}).collect(Collectors.toList());

			sources.forEach((k,v) -> javaFileObjects.add(MemoryJavaFileManager.createJavaFileObject(k, v)));

			// C) Java compiler options
			List<String> options = createCompilerOptions();

			PrintWriter err = new PrintWriter(System.err);
			log.info("compiling {} classes", javaFileObjects.size());
			// Create a compilation task.
			CompilationTask compilationTask = javaCompiler.getTask(err, javaFileManager, diagnosticCollector, options,
					null, javaFileObjects);

			String result = "Succes\n";
			if (!compilationTask.call()) {
				StringBuilder sb = new StringBuilder();
				for (Diagnostic<?> diagnostic : diagnosticCollector.getDiagnostics())
					sb.append(report(diagnostic));
				result = sb.toString();
				System.out.println(result);
				diagnosticCollector = new DiagnosticCollector<>();
				return new CompileResult(result, javaFileManager.getMemoryMap(), user, false);
			}

			return new CompileResult(result, javaFileManager.getMemoryMap(), user, true);
		};
		return supplier;
	}

	private List<String> createCompilerOptions() {
		List<String> options = new ArrayList<String>();
		// enable all recommended warnings.
		options.add("-Xlint:all");
		// enable debugging for line numbers and local variables.
		options.add("-g:lines,vars");

		return options;
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
