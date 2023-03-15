package nl.moj.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static boolean isZip(InputStream in) {
        try {
            new ZipInputStream(in).getNextEntry();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean containsSingleFolder(InputStream in) {
        int topLevelFolders = 0;
        try (ZipInputStream zin = new ZipInputStream(in)) {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory() && ze.getName().chars().filter(ch -> ch == '/').count() == 1) {
                    topLevelFolders += 1;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return topLevelFolders == 1;
    }

    public static Path getFirstDirectoryName(InputStream in) {
        try (ZipInputStream zin = new ZipInputStream(in)) {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    return Path.of(ze.getName());
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

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

    public static void unzip(InputStream in, Path dest) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(in)) {
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

    public static void unzip(Path source, Path dest) throws IOException {
        try (InputStream in = Files.newInputStream(source, StandardOpenOption.READ)) {
            unzip(in, dest);
        }
    }
}
