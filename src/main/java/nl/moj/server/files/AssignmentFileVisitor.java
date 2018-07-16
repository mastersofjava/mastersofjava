package nl.moj.server.files;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.Competition;
import nl.moj.server.model.Test;
import nl.moj.server.repository.TeamRepository;
import nl.moj.server.repository.TestRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@RequiredArgsConstructor
public class AssignmentFileVisitor extends SimpleFileVisitor<Path> {

    private final String DIRECTORY;

    private final Competition competition;

    private final TestRepository testRepository;

    private final TeamRepository teamRepository;

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String filename = file.getFileName().toString();
        String fileAndDir = file.toString();
        String type = filename.substring(filename.indexOf("."));

        if (".md".equalsIgnoreCase(type)) {
            return super.visitFile(file, attrs);
        }

        String content = new String(Files.readAllBytes(file));
        int beginIndex = fileAndDir.indexOf(DIRECTORY) + DIRECTORY.length() + 1;
        int indexOf = fileAndDir.indexOf(System.getProperty("file.separator"), beginIndex);
        String assignment = fileAndDir.substring(beginIndex, indexOf);

        File origFile = file.toFile();
        AssignmentFile assignmentFile = null;

        switch (type) {
            case ".java":
                if (competition.getAssignment(assignment).getEditableFileNames().contains(filename)) {
                    assignmentFile = new AssignmentFile(filename, content, FileType.EDIT, assignment, origFile);
                } else if (competition.getAssignment(assignment).getTestFileNames().contains(filename)) {
                    assignmentFile = new AssignmentFile(filename, content, FileType.TEST, assignment, origFile);
                    teamRepository.findAllByRole("ROLE_USER").forEach(team -> testRepository.save(new Test(team, assignment,
                            filename.substring(0, filename.indexOf(".")), 0, 0)));
                } else if (competition.getAssignment(assignment).getSubmitFileNames().contains(filename)) {
                    assignmentFile = new AssignmentFile(filename, content, FileType.SUBMIT, assignment, origFile);
                } else if (competition.getAssignment(assignment).getSolutionFileNames().contains(filename)) {
                    assignmentFile = new AssignmentFile(filename, content, FileType.SOLUTION, assignment, origFile);
                } else {
                    assignmentFile = new AssignmentFile(filename, content, FileType.READONLY, assignment, origFile);
                }
                competition.addAssignmentFile(assignmentFile);
                break;
            case ".txt":
                assignmentFile = new AssignmentFile(filename, content, FileType.TASK, assignment, origFile);
                competition.addAssignmentFile(assignmentFile);
                break;
            case ".xml":
                if (filename.equalsIgnoreCase("pom.xml")) {
                    assignmentFile = new AssignmentFile(filename, content, FileType.POM, assignment, origFile);
                    competition.addAssignmentFile(assignmentFile);
                }
            default:
                break;
        }

        return super.visitFile(file, attrs);
    }

}
