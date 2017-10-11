package nl.moj.server.competition;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AssignmentRepositoryService {

	private static final Logger log = LoggerFactory.getLogger(AssignmentRepositoryService.class);

	@Value("${moj.server.gitrepository}")
	private String gitrepository;

	@Value("${moj.server.branch}")
	private String branch;

	@Value("${moj.server.assignmentDirectory}")
	private String assignmentDirectory;

	public boolean cloneRemoteGitRepository()  {
		File tmpPath;
		try {
			tmpPath = File.createTempFile("gitrepo", "");
			if (!tmpPath.delete()) {
				log.error("Could not delete temporary file " + tmpPath);
			}
			// then clone
			log.info("Cloning from " + gitrepository + " to " + tmpPath);

			Git result;
			try {
				result = Git.cloneRepository().setURI(gitrepository).setDirectory(tmpPath).call();
				result.checkout().setName(branch).call();
				result.close();
			} catch (GitAPIException e) {
				log.error(e.getMessage(), e);
				return false;
			}
			File assignmentDir = new File(assignmentDirectory);
			// make sure its empty
			emptyDir(assignmentDir);
			// copy to assignments dir
			copyDir(tmpPath.toPath(), assignmentDir.toPath());
			log.info("Copied assignments to " + assignmentDir);
			return true;
		} catch (IOException e) {
			log.error(e.getMessage(),e);
			return false;
		}
	}

	private void copyDir(final Path sourcePath, final Path targetPath) {
		try {
			Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
						throws IOException {
					if (dir.endsWith(".git")) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (file.endsWith(".gitignore")) {
						return FileVisitResult.CONTINUE;
					}
					Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
	}

	private void emptyDir(File path) {
		try {
			Files.walkFileTree(path.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					System.out.println(dir);
					if (dir.endsWith("assignments"))
						return FileVisitResult.CONTINUE;
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

}
