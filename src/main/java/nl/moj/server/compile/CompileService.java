package nl.moj.server.compile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
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

	@Autowired
	private Executor timed;

	@AsyncTimed
	@Async("timed")
	public CompletableFuture<CompileResult> compile(List<String> teamOpgave) {
		return CompletableFuture.supplyAsync(new Supplier<CompileResult>() {

			@Override
			public CompileResult get() {

				final List<String> editableFileNames = assignmentService.getEditableFileNames();
				final List<JavaFile> assignmentFiles = assignmentService.getAssignmentFiles();
				List<JavaFileObject> javaFileObjects = assignmentFiles.stream()
						.filter(a -> !assignmentService.getEditableFileNames().contains(a.getName())).map(a -> {
							JavaFileObject jfo = MemoryJavaFileManager.createJavaFileObject(a.getFilename(),
									a.getContent());
							return jfo;
						}).collect(Collectors.toList());
				editableFileNames.forEach(file -> javaFileObjects.add(MemoryJavaFileManager.createJavaFileObject(file,
						teamOpgave.get(editableFileNames.indexOf(file)))));

				// C) Java compiler options
				List<String> options = createCompilerOptions();

				PrintWriter err = new PrintWriter(System.err);

				// Create a compilation task.
				CompilationTask compilationTask = javaCompiler.getTask(err, javaFileManager, diagnosticCollector,
						options, null, javaFileObjects);

				String result = "Succes\n";
				if (!compilationTask.call()) {
					StringBuilder sb = new StringBuilder();
					for (Diagnostic<?> diagnostic : diagnosticCollector.getDiagnostics())
						report(diagnostic, sb);
					result = sb.toString();
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return new CompileResult(result, javaFileManager.getMemoryMap());
			}
		}, timed);
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
