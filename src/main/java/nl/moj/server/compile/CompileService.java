package nl.moj.server.compile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.AssignmentService;
import nl.moj.server.JavaFile;
import nl.moj.server.timed.AsyncTimed;

@Service
public class CompileService {

	@Autowired
	private javax.tools.JavaCompiler javaCompiler;
	@Autowired
	private DiagnosticCollector<JavaFileObject> diagnosticCollector;
	@Autowired
	private MemoryJavaFileManager<StandardJavaFileManager> javaFileManager;

	@Autowired
	private AssignmentService assignmentService;

	@AsyncTimed
	public CompletableFuture<CompileResult> compile(String teamOpgave) {

		return CompletableFuture.supplyAsync(new Supplier<CompileResult>() {

			@Override
			public CompileResult get() {
				List<JavaFile> assignmentFiles = assignmentService.getAssignmentFiles();
				List<JavaFileObject> javaFileObjects = assignmentFiles.stream()
						.filter(a -> !a.getName().equals("WorkloadbalancerImpl")).map(a -> {
							JavaFileObject jfo = MemoryJavaFileManager.createJavaFileObject(a.getFilename(), a.getContent());
							return jfo;
						}).collect(Collectors.toList());

				javaFileObjects.add(MemoryJavaFileManager.createJavaFileObject("WorkloadbalancerImpl.java", teamOpgave));

				// C) Java compiler options
				List<String> options = createCompilerOptions();

				PrintWriter err = new PrintWriter(System.err);

				// Create a compilation task.
				CompilationTask compilationTask = javaCompiler.getTask(err, javaFileManager, diagnosticCollector, options, null,
						javaFileObjects);
				String result = "Succes\n";
				// Performs this compilation task.
				// True, if and only if, all the files compiled without errors.
				if (!compilationTask.call()) {
					StringBuilder sb = new StringBuilder();
					for (Diagnostic<?> diagnostic : diagnosticCollector.getDiagnostics())
						report(diagnostic, sb);
					result = sb.toString();
				}
				return new CompileResult(result, javaFileManager.getMemoryMap());
			}
		});
	}
	private List<String> createCompilerOptions() {
		List<String> options = new ArrayList<String>();
		// enable all recommended warnings.
		options.add("-Xlint:all");
		// enable debugging for line numbers and local variables.
		options.add("-g:lines,vars");
		return options;
	}

	private String report(Diagnostic<?> dg, StringBuilder sb) {
		sb.append(dg.getKind() + "> Line=" + dg.getLineNumber() + ", Column=" + dg.getColumnNumber() + "\n");
		sb.append("Message> " + dg.getMessage(null) + "\n");
		sb.append("Cause> " + dg.getCode() + "\n");
		return sb.toString();
	}
}
