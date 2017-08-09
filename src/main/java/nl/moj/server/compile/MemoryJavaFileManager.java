package nl.moj.server.compile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import javax.tools.JavaFileManager.Location;

/**
 * <p>
 * This class is a JavaFileManager to store compiled byte code in a memory map.
 * </p>
 * 
 * <p>
 * A JavaFileManager determines where to create new JavaFileObjects. In this
 * context, <em>file</em> means an abstraction of regular files and other
 * sources of data. For example, a file object can be used to represent regular
 * files, memory cache, or data in databases.
 * </p>
 * 
 * <p>
 * Modified from the original to:
 * </p>
 * 
 * <ul>
 * <li>Format source.</li>
 * <li>Organize imports.</li>
 * <li>Use generics.</li>
 * <li>Change URI namespace to org/patrodyne/scripting.</li>
 * </ul>
 * 
 * @author A. Sundararajan
 * @author Rick O'Sullivan
 */
@Component
public final class MemoryJavaFileManager<FM extends JavaFileManager> extends ForwardingJavaFileManager<FM> {
	/** Java source file extension. */
	private final static String JAVA_SOURCE_EXTENSION = ".java";

	private Map<String, byte[]> memoryMap;

	/**
	 * Get a mapping of class name and byte code pairs.
	 * 
	 * @return A memory map of class names and byte code.
	 */
	public Map<String, byte[]> getMemoryMap() {
		if (memoryMap == null)
			setMemoryMap(new HashMap<String, byte[]>());
		return memoryMap;
	}

	private void setMemoryMap(Map<String, byte[]> memoryMap) {
		this.memoryMap = memoryMap;
	}

	/**
	 * Creates a new instance of MemoryJavaFileManager.
	 * 
	 * @param fileManager
	 *            A delegate to this file manager
	 */
	public MemoryJavaFileManager(FM fileManager) {
		super(fileManager);
	}

	/**
	 * Releases any resources opened by this file manager directly or indirectly.
	 * This might render this file manager useless and the effect of subsequent
	 * calls to methods on this object or any objects obtained through this object
	 * is undefined unless explicitly allowed. However, closing a file manager which
	 * has already been closed has no effect.
	 *
	 * @throws IOException
	 *             if an I/O error occurred
	 * @see #flush
	 */
	public void close() throws IOException {
		setMemoryMap(null);
		super.close();
	}

	/**
	 * Flushes any resources opened for output by this file manager directly or
	 * indirectly. Flushing a closed file manager has no effect.
	 *
	 * @throws IOException
	 *             if an I/O error occurred
	 * @see #close
	 */
	public void flush() throws IOException {
		super.flush();
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

		public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
			return CharBuffer.wrap(sourceCode);
		}
	}

	/**
	 * A subclass of JavaFileObject used to output a Java target to a local class
	 * map.
	 */
	private class TargetJavaFileObject extends SimpleJavaFileObject {
		private String className;

		protected TargetJavaFileObject(String name) {
			super(toURI(name), Kind.CLASS);
			this.className = name;
		}

		/**
		 * An output stream to store byte code into the local class map.
		 */
		public OutputStream openOutputStream() {
			return new FilterOutputStream(new ByteArrayOutputStream()) {
				public void close() throws IOException {
					out.close();
					ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
					getMemoryMap().put(className, bos.toByteArray());
				}
			};
		}
	}

	/**
	 * Gets a {@linkplain JavaFileObject file object} for output representing the
	 * specified class of the specified kind in the given location.
	 * 
	 * @param location
	 *            a location to search for file objects.
	 * @param className
	 *            the name of a class.
	 * @param kind
	 *            the kind of file, must be one of {@link JavaFileObject.Kind#SOURCE
	 *            SOURCE} or {@link JavaFileObject.Kind#CLASS CLASS}
	 * @param sibling
	 *            a file object to be used as hint for placement; might be
	 *            {@code null}
	 *
	 * @return a file object for output
	 */
	public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, Kind kind,
			FileObject sibling) throws IOException {
		if (kind == Kind.CLASS)
			return new TargetJavaFileObject(className);
		else
			return super.getJavaFileForOutput(location, className, kind, sibling);
	}

//	public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
//		if (location == StandardLocation.CLASS_OUTPUT && getMemoryMap().containsKey(className) && kind == Kind.CLASS) {
//			byte[] bs = getMemoryMap().get(className);
//			return new SimpleJavaFileObject(URI.create(className), kind) {
//				@NotNull
//				public InputStream openInputStream() {
//					return new ByteArrayInputStream(bs);
//				}
//			};
//		}
//
//		return fileManager.getJavaFileForInput(location, className, kind);
//
//	}

//    public ClassLoader getClassLoader(Location location) {
//    	if (location == StandardLocation.CLASS_OUTPUT) {
//    		new MemoryClassLoader(getMemoryMap());
//    	}
//    	
//        return fileManager.getClassLoader(location);
//    }
    
    
	/**
	 * Create a JavaFileObject for the given class name and byte code.
	 * 
	 * @param className
	 *            The name to identify the class.
	 * @param bytecode
	 *            The class byte code.
	 * 
	 * @return A new JavaFileObject containing the bytcode and identified by the
	 *         class name.
	 */
	protected static JavaFileObject createJavaFileObject(String className, String sourceCode) {
		return new SourceJavaFileObject(className, sourceCode);
	}

	private static URI toURI(String name) {
		File file = new File(name);
		if (file.exists())
			return file.toURI();
		else {
			try {
				final StringBuilder newUri = new StringBuilder();
				newUri.append("mfm:///");
				newUri.append(name.replace('.', '/'));

				if (name.endsWith(JAVA_SOURCE_EXTENSION))
					newUri.replace(newUri.length() - JAVA_SOURCE_EXTENSION.length(), newUri.length(),
							JAVA_SOURCE_EXTENSION);

				return URI.create(newUri.toString());
			} catch (Exception exp) {
				return URI.create("mfm:///org/patrodyne/scripting/java/java_source");
			}
		}
	}
}
