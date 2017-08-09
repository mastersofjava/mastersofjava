package nl.moj.server.compile;

import static java.lang.String.format;

import java.util.Map;

public class MemoryClassLoader extends ClassLoader {
    private final Map<String, byte[]> classDefinitions;

    public MemoryClassLoader(Map<String, byte[]> classDefinitions) {
        this.classDefinitions = classDefinitions;
    }

    @Override
    public Class findClass(String name) throws ClassNotFoundException {
        byte[] b = classDefinitions.get(name);
        if (b == null)
            throw new ClassNotFoundException(format("Could not find the class %s using the in memory cache.", name));
        return defineClass(name, b, 0, b.length);
    }
}
