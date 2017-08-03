package nl.moj.server.compile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.AssignmentService;
import nl.moj.server.JavaFile;

import javax.annotation.PostConstruct;
import javax.tools.Diagnostic;
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

    //@PostConstruct
//    public Stream<DiagnosticDTO> compile() throws IOException {
//        AssignmentDTO assignmentDTO = AssignmentDTO.of("team1", "SimplePuzzle");
//
//        return compile(assignmentDTO);
//    }

    public void compile(AssignmentDTO assignment) throws IOException {
        //final Path root = workDir().resolve(assignment.getTeamName()).resolve(assignment.getAssignment());

        createClassPath(true);
        //prepareOutputDirectory(root);

        List<JavaFile> assignmentFiles = assignmentService.getAssignmentFiles();
        JavaFile opgave = assignmentFiles.stream().filter(f-> f.getName().equalsIgnoreCase("opgave")).findFirst().get();
        
        
        compiler.compile(opgave.getFilename(), opgave.getContent());//, err, sourcePath, classPath);
        
        
//        final Iterable<? extends JavaFileObject> javaObjectFiles = standardFileManager.getJavaFileObjectsFromFiles(assignmentFiles(root));

        //TODO: move compilation to a separate thread!
//        final JavaCompiler.CompilationTask task = compiler.getTask(null, standardFileManager, diagnosticCollector,null, null, javaObjectFiles);
//        final Boolean call = task.call();

      //  return diagnosticCollector
       //         .getDiagnostics()
      //          .stream()
      //          .map(d -> DiagnosticDTO.of(d.getSource().getName(), d.getLineNumber(), d.getMessage(null)));
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
