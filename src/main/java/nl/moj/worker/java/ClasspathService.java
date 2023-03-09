package nl.moj.worker.java;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClasspathService {

    private final MojServerProperties mojServerProperties;

    public String resolveClasspath(Collection<Path> paths) {
        final List<Path> classPath = new ArrayList<>();
        if (paths != null && !paths.isEmpty()) {
            classPath.addAll(paths);
        }
        classPath.add(resolveLibrary("junit-4.12.jar"));
        classPath.add(resolveLibrary("hamcrest-all-1.3.jar"));
        classPath.add(resolveLibrary("asciiart-core-1.1.0.jar"));

        for (Path file : classPath) {
            if (Files.exists(file)) {
                log.trace("found: {}", file.toAbsolutePath());
            } else {
                log.error("not found: {}", file.toAbsolutePath());
            }
        }
        return classPath.stream().map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.joining(File.pathSeparator));
    }

    private Path resolveLibrary(String library) {
        return mojServerProperties.getDirectories().getBaseDirectory()
                .resolve(mojServerProperties.getDirectories().getLibDirectory()).resolve(library);
    }

}
