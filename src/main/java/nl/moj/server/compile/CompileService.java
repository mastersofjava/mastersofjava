package nl.moj.server.compile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.AssignmentService;
import nl.moj.server.JavaFile;

import javax.annotation.PostConstruct;
import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class CompileService {

	@Autowired
    private JavaCompiler compiler;

    private DiagnosticCollector<JavaFileObject> diagnosticCollector;

    @Autowired
    private CompilerProperties compilerProperties;
    
    @Autowired
    private AssignmentService assignmentService;

    private List<File> createClassPath(final boolean inheritClassPath) {

        final Path homeDir = Paths.get(System.getProperty("user.home"));
        final Path mavenRepo = homeDir.resolve(".m2").resolve("repository");

        final List<File> classPath = new ArrayList<>();

//        if(inheritClassPath) {
//            final Iterable<? extends File> location = standardFileManager.getLocation(StandardLocation.CLASS_PATH);
//            location.forEach(classPath::add);
//        }

        final Set<File> additionalClassPathEntries = compilerProperties.getClassPath()
                .stream()
                .map(mavenRepo::resolve)
                .map(Path::toFile)
                .collect(Collectors.toSet());

        classPath.addAll(additionalClassPathEntries);

        return classPath;
    }

    public String compile(String teamOpgave) throws IOException {
        createClassPath(true);

        List<JavaFile> assignmentFiles = assignmentService.getAssignmentFiles();
        JavaFile order = assignmentFiles.stream().filter(f-> f.getName().equalsIgnoreCase("Order")).findFirst().get();
        JavaFile test = assignmentFiles.stream().filter(f-> f.getName().equalsIgnoreCase("Test")).findFirst().get();
        JavaFile opgave = new JavaFile("WorkloadbalancerImpl.java", teamOpgave);
        
		List<JavaFileObject> javaFileObjects = new ArrayList<JavaFileObject>(1);
		javaFileObjects.add(MemoryJavaFileManager.createJavaFileObject(order.getFilename(), order.getContent()));
		javaFileObjects.add(MemoryJavaFileManager.createJavaFileObject(opgave.getFilename(), opgave.getContent()));
		javaFileObjects.add(MemoryJavaFileManager.createJavaFileObject(test.getFilename(), test.getContent()));
		return compiler.compile(javaFileObjects,null,null);//, err, sourcePath, classPath);
		
    }

    private Set<File> assignmentFiles(final Path root) throws IOException {
        final Set<File> javaFiles = new HashSet<>();

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException {

                if(path.toFile().isFile() && path.toString().endsWith(".java")) {
                    javaFiles.add(path.toFile());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return javaFiles;
    }

    private void prepareOutputDirectory(final Path workDir) throws IOException {
        final File outputDir = workDir.resolve(outputDir()).toFile();

        if(!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create output directory: " + outputDir);
        }

        //standardFileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir));
    }

    private Path workDir() {
        return compilerProperties.getWorkDir();
    }

    private Path outputDir() {
        return compilerProperties.getOutputDir();
    }

}
