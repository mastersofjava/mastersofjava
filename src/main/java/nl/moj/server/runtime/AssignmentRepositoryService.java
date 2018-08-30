package nl.moj.server.runtime;

import nl.moj.server.AssignmentRepoConfiguration;
import nl.moj.server.AssignmentRepoConfiguration.Repo;
import nl.moj.server.DirectoriesConfiguration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@Service
public class AssignmentRepositoryService {

	private static final Logger log = LoggerFactory.getLogger(AssignmentRepositoryService.class);

	private AssignmentRepoConfiguration repos;

	private DirectoriesConfiguration directories;
	
	public AssignmentRepositoryService(AssignmentRepoConfiguration repos, DirectoriesConfiguration directories) {
		super();
		this.repos = repos;
		this.directories = directories;
	}

	public boolean cloneRemoteGitRepository(String repoName) {
		File tmpPath;
		try {
			tmpPath = File.createTempFile("gitrepo", "");
			if (!tmpPath.delete()) {
				log.error("Could not delete temporary file " + tmpPath);
			}
			Repo repo = repos.getRepos().stream().filter(r -> r.getName().equalsIgnoreCase(repoName)).findFirst().get();
			// then clone
			log.info("Cloning from " + directories.getBaseDirectory() + "/" + repo.getUrl() + " to " + tmpPath);

			Git result;
			try {
				result = Git.cloneRepository().setURI(repo.getUrl()).setDirectory(tmpPath).call();

				result.checkout().setName(repo.getBranch()).call();
				result.close();
			} catch (GitAPIException e) {
				log.error(e.getMessage(), e);
				return false;
			}
			File assignmentDir = new File(directories.getBaseDirectory(), directories.getAssignmentDirectory());
			// make sure its empty
			emptyDir(assignmentDir);
			// copy to assignments dir
			copyDir(tmpPath.toPath(), assignmentDir.toPath());
			log.info("Copied assignments to " + assignmentDir);
			return true;
		} catch (IOException e) {
			log.error(e.getMessage(), e);
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
			log.error(e.getMessage(), e);
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
					if (dir.endsWith(directories.getAssignmentDirectory()))
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
