package nl.moj.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;
import nl.moj.server.util.ZipFileReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UploadController {

    private final CompetitionService competitionService;

    private final MojServerProperties mojServerProperties;

    private AssignmentLocationCache assignmentLocationCache;

    private class AssignmentLocationCache {
        private List<File> fileList = new ArrayList<>();
        public AssignmentLocationCache() {
            initCache();
        }
        private void initCache() {
            fileList.clear();
            for (File rootFile: mojServerProperties.getAssignmentRepo().getParent().toFile().listFiles()) {
                for (File assignment: rootFile.listFiles()) {
                    if (assignment.isDirectory()) {
                        fileList.add(assignment);
                    }
                }
            }
        }
        public File getAssignmentLocation(String name) {
            for (File file: fileList) {
                if (file.getName().equals(name)) {
                    return file;
                }
            }
            initCache();
            return null;
        }
    }

    private void validateImportedSmallFile(MultipartFile file) throws IOException {
        Assert.isTrue(file.getBytes().length > 0, "empty input given: " + file.getOriginalFilename());
        Assert.isTrue(file.getBytes().length < 1000 * 1000, "max size of importfile exceeded");
        log.info("import file " + file + " size " + file.getBytes().length + " " + file.getOriginalFilename() + " " + file.getContentType());
    }

    /**
     * import users (for demostrating large groups of users)
     * @param file (csv file with names)
     * @param redirectAttributes - admin feedback redirected on control page.
     */
    @PostMapping(value = "/importUsers", consumes = {"multipart/form-data"})
    @RolesAllowed({Role.ADMIN})
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            validateImportedSmallFile(file);
            String[] lines = new String(file.getBytes()).split("\n");
            log.info("lines " + lines.length);
            competitionService.importTeams(Arrays.asList(lines));
            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded " + file.getOriginalFilename() + "!");
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("error",
                    "Upload failed!");
        }

        return "redirect:/control";
    }


    private void validateValidZipFileForLocation(MultipartFile file) {
        Assert.isTrue(file.getContentType().contains("zip"), "invalid contenttype " + file.getContentType());
        mojServerProperties.getAssignmentRepo().toFile().mkdirs();
        Assert.isTrue(mojServerProperties.getAssignmentRepo().toFile().exists(), "location invalid " + mojServerProperties.getAssignmentRepo());
    }
    private void validateValidAssignmentStorage(String name) {
        String[] parts = name.split("-");
        Assert.isTrue(StringUtils.isNumeric(parts[0]) && name.contains("assignments"), "incorrect name format:" + name);
    }

    /**
     * with this method one can upload/overwrite assignments on a server.
     * @param file - zip file, should be a valid assignment storage.
     * @param redirectAttributes - admin feedback redirected on control page.
     */
    @PostMapping(value = "/importAssignments", consumes = {"multipart/form-data"})
    @RolesAllowed({Role.ADMIN})
    public String importAssignments(@RequestParam("file") MultipartFile file,
                                    RedirectAttributes redirectAttributes) {
        try {
            validateImportedSmallFile(file);
            if (!mojServerProperties.getAssignmentRepo().toFile().exists()) {
                mojServerProperties.getAssignmentRepo().toFile().mkdirs();
            }

            validateValidZipFileForLocation(file);
            validateValidAssignmentStorage(file.getOriginalFilename());
            File outputFile = new File(mojServerProperties.getAssignmentRepo().toFile().getParentFile(), file.getOriginalFilename());

            FileUtils.writeByteArrayToFile(outputFile, file.getBytes());
            log.info("unzipit quickly " + outputFile);
            ZipFileReader.unZipIt(outputFile.getPath());
            outputFile.delete();
            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded " + file.getOriginalFilename() + "!");
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("error",
                    "Upload failed!");
        }

        return "redirect:/control";
    }

    private AssignmentLocationCache getAssignmentLocationCache() {
        if (assignmentLocationCache==null) {
            assignmentLocationCache = new AssignmentLocationCache();
        }
        return assignmentLocationCache;
    }

    /**
     * this method ensures that one can place images in the assets of an assignment.
     * sample usage would be: <img src='/public/assignment_image/moj-HexagonalChess/public_Hexagonal_chess.svg'>
     * @param assignment - the assignment name
     * @param file_name - the file name of the image that should be render
     * @param response - the available httpResponse
     */
    @GetMapping("/public/assignment_image/{assignment}/{file_name}")
    public void insertAssignmentImage(@PathVariable("assignment") String assignment, @PathVariable("file_name") String file_name, HttpServletResponse response) {
        Assert.isTrue(file_name.contains("public"),"invalid request1");
        File assignmentFile = getAssignmentLocationCache().getAssignmentLocation(assignment);
        Assert.isTrue(assignmentFile !=null && assignmentFile.isDirectory(),"invalid request2");
        File sourceFile = new File(assignmentFile, "/assets/" +file_name);
        response.setContentType( getImageContentType(file_name));
        try {
            FileUtils.copyFile(sourceFile, response.getOutputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String getImageContentType(String fn) {
        String contentType = "text/html";
        if (fn.endsWith("svg")) {
            contentType = "image/svg+xml";
        }
        if (fn.endsWith("pdf")) {
            contentType = "application/pdf";
        }
        if (fn.endsWith("jpg") || fn.endsWith("jpeg")) {
            contentType = "image/jpeg";
        }
        if (fn.endsWith("png")) {
            contentType = "image/png";
        }
        if (fn.endsWith("txt")) {
            contentType = "text/plain";
        }
        if (fn.endsWith("bmp")) {
            contentType = "image/bmp";
        }
        if (fn.endsWith("gif")) {
            contentType = "image/gif";
        }
        return contentType;
    }
}
