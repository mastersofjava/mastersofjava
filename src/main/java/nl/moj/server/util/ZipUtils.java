package nl.moj.server.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void zip(Path source, Path dest) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dest, StandardOpenOption.WRITE))) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(source.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public static void unzip(Path source, Path dest) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(source, StandardOpenOption.READ))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                Path p = dest.resolve(zipEntry.getName());
                if (!p.normalize().startsWith(dest)) {
                    throw new IOException("Unzip entry " + p.toAbsolutePath() + " writing outside target folder " + dest.toAbsolutePath());
                }

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(p);
                } else {
                    if (p.getParent() != null) {
                        if (Files.notExists(p.getParent())) {
                            Files.createDirectories(p.getParent());
                        }
                    }
                    Files.copy(zipInputStream, p, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
        }
    }
}
