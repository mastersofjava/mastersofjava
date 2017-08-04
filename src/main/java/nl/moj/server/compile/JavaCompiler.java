package nl.moj.server.compile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Simple interface to Java compiler using JSR 199 Compiler API.
 * 
 * Modified from the original to:
 * 
 * <ul>
 * <li>Format source.</li>
 * <li>Organize imports.</li>
 * <li>Parameterize raw types.</li>
 * <li>Reference makeStringSource statically.</li>
 * </ul>
 * 
 * @author A. Sundararajan
 * @author Rick O'Sullivan
 */
@Service
public class JavaCompiler {

	private final javax.tools.JavaCompiler systemJavaCompiler;

	private MemoryJavaFileManager<StandardJavaFileManager> javaFileManager;

	public JavaCompiler(javax.tools.JavaCompiler systemJavaCompiler,
			MemoryJavaFileManager<StandardJavaFileManager> javaFileManager) {
		this.systemJavaCompiler = systemJavaCompiler;
		this.javaFileManager = javaFileManager;
	}

	public String compile(List<JavaFileObject> javaFileObjects, String sourcePath, String classPath) {
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		// C) Java compiler options
		List<String> options = createCompilerOptions(sourcePath, classPath);

		PrintWriter err = new PrintWriter(System.err);

		// Create a compilation task.
		CompilationTask compilationTask = systemJavaCompiler.getTask(err, javaFileManager, diagnostics, options, null,
				javaFileObjects);

		// Performs this compilation task.
		// True, if and only if, all the files compiled without errors.
		if (!compilationTask.call()) {
			StringBuilder sb = new StringBuilder();
			for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics())
				report(diagnostic, sb);
			return sb.toString();
		}
		return "Succes";
	}

	/**
	 * Report a compiler diagnostic to the given print writer.
	 * 
	 * @param dg
	 *            A complier diagnostic.
	 * @param pw
	 *            A print writer.
	 */
	protected String report(Diagnostic<?> dg, StringBuilder sb) {

		sb.append(dg.getSource());
		sb.append(dg.getKind() + "> Line=" + dg.getLineNumber() + ", Column=" + dg.getColumnNumber());
		sb.append("Message> " + dg.getMessage(null));
		return sb.toString();
	}

	/**
	 * <p>
	 * Create Java compiler options for the given source path and class path.
	 * </p>
	 * 
	 * <p>
	 * Options include:
	 * </p>
	 * <ul>
	 * <li><code>-Xlint:all</code> - enable all recommended warnings.</li>
	 * <li><code>-g:lines,vars</code> - enable debugging for line numbers and local
	 * variables.</li>
	 * <li><code>-sourcepath</code> - colon separated list of paths for source code
	 * resolution.</li>
	 * <li><code>-classpath</code> - colon separated list of paths or jars for class
	 * resolution.</li>
	 * </ul>
	 * 
	 * @param sourcePath
	 *            A colon separated list of paths for source code resolution.
	 * @param classPath
	 *            A colon separated list of paths or jars for class resolution.
	 * 
	 * @return A list of compiler options as individual strings.
	 */
	protected List<String> createCompilerOptions(String sourcePath, String classPath) {
		List<String> options = new ArrayList<String>();

		// enable all recommended warnings.
		options.add("-Xlint:all");

		// enable debugging for line numbers and local variables.
		options.add("-g:lines,vars");

		// Specify the source code path to search for class or interface definitions.
		// As with the user class path, source path entries are separated by colons (:)
		// and
		// can be directories, JAR archives, or ZIP archives. If packages are used, the
		// local path name within the directory or archive must reflect the package
		// name.
		if (sourcePath != null) {
			options.add("-sourcepath");
			options.add(sourcePath);
		}

		// Class path entries are separated by colons (:) and can be directories, JAR
		// archives,
		// ZIP archives or .class files. Each classpath entry should end with a filename
		// or
		// directory; otherwise, the entry will be ignored.
		if (classPath != null) {
			options.add("-classpath");
			options.add(classPath);
		}
		return options;
	}
}