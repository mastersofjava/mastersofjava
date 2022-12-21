package nl.moj.server.compiler.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.submit.model.SourceMessage;

@Slf4j
public class TeamProjectPathModel {
    /**
	 * 
	 */
	private final CompileService compileService;
	Path teamAssignmentDir;
    private Path sourcesDir;
    Path classesDir;
    String errorMessage;

    public TeamProjectPathModel(CompileService compileService, UUID teamUUID, Assignment assignment, ActiveAssignment state) {
        this.compileService = compileService;
		teamAssignmentDir = this.compileService.teamService.getTeamAssignmentDirectory(state.getCompetitionSession(), teamUUID, assignment);
        sourcesDir = teamAssignmentDir.resolve("sources");
        classesDir = teamAssignmentDir.resolve("classes");
    }

    public boolean cleanCompileLocationForTeam() {
        boolean isValidCleanStart = false;
        try {
            if (teamAssignmentDir.toFile().exists()) {

                Collection<File> fileList = FileUtils.listFiles(teamAssignmentDir.toFile(), new String[]{"java", "class"}, true);

                for (File file : fileList) {
                    if (file.exists()) {
                        FileUtils.deleteQuietly(file);
                        File project = file.getParentFile().getParentFile();
                        FileUtils.deleteQuietly(project);
                    }
                }
                isValidCleanStart = !teamAssignmentDir.toFile().exists() || teamAssignmentDir.toFile().list().length == 0;
            } else {
                isValidCleanStart = true;
            }
        } catch (Exception e) {
            log.error("error while cleaning teamdir: " + teamAssignmentDir.toFile(), e);
        }
        boolean isValidSources = sourcesDir.toFile().mkdirs();
        boolean isValidClasses = classesDir.toFile().mkdirs();
        isValidCleanStart &= isValidSources && isValidClasses;
        log.info("cleanedDirectory: {} isValidCleanStart {}", teamAssignmentDir, isValidCleanStart);
        log.info("sources created? -> {} isValidSources {}", sourcesDir, isValidSources);
        log.info("classes created? -> {} isValidClasses {}", classesDir, isValidClasses);
        return isValidCleanStart;
    }

    public void destroy() {
        sourcesDir = null;
        classesDir = null;
        teamAssignmentDir = null;
    }

    void prepareResources(List<AssignmentFile> resources) {
        resources.forEach(r -> {
            try {
                File target = this.classesDir.resolve(r.getFile()).toFile();
                File parentFile = target.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                    target.getParentFile().mkdirs();
                }
                FileUtils.copyFile(r.getAbsoluteFile().toFile(), target);
                log.debug(" - copied {}", r);
            } catch (IOException e) {
                log.error("error while writing resources to classes dir", e);
                this.errorMessage = e.getMessage();
            }
        });
    }

    void prepareInputSources(List<AssignmentFile> assignmentFiles, SourceMessage message, CompileInputWrapper compileInputWrapper) {
        message.getSources().forEach((uuid, v) -> {
            try {
                AssignmentFile orig = compileInputWrapper.getOriginalAssignmentFile(uuid);
                File f = this.sourcesDir.resolve(orig.getFile()).toFile();
                File parentFile = f.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                Files.deleteIfExists(f.toPath());
                FileUtils.writeStringToFile(f, v, StandardCharsets.UTF_8);
                
                log.debug(" - writing input sources to {}", f);
                assignmentFiles.add(orig.toBuilder()
                        .absoluteFile(f.toPath())
                        .build());
                

            } catch (IOException | RuntimeException e) {
                log.error("error while writing sourcefiles to sources dir", e);
                this.errorMessage = e.getMessage();
            }

        });
        Assert.isTrue(this.errorMessage == null, this.errorMessage);
    }
}