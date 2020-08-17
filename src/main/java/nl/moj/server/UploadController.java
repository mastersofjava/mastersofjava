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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.security.RolesAllowed;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UploadController {

    private final CompetitionService competitionService;

    private final MojServerProperties mojServerProperties;


    private void validateImportedSmallFile(MultipartFile file) throws IOException {
        Assert.isTrue(file.getBytes().length>0,"empty input given: " +file.getOriginalFilename());
        Assert.isTrue(file.getBytes().length<1000*1000,"max size of importfile exceeded");
        log.info("import file " + file + " size " + file.getBytes().length + " " +file.getOriginalFilename() + " " + file.getContentType());
    }

    @PostMapping(value="/importUsers", consumes = {"multipart/form-data"})
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
            log.error(ex.getMessage(),ex);
            redirectAttributes.addFlashAttribute("error",
                    "Upload failed!");
        }

        return "redirect:/control";
    }


    private void validateValidZipFileForLocation(MultipartFile file) {
        Assert.isTrue(file.getContentType().contains("zip"),"invalid contenttype " +file.getContentType());
        Assert.isTrue(mojServerProperties.getAssignmentRepo().toFile().exists(),"location invalid " +mojServerProperties.getAssignmentRepo());
    }
    private void validateValidAssignmentStorage(String name) {
        String[] parts = name.split("-");
        Assert.isTrue(StringUtils.isNumeric(parts[0]) && name.contains("assignments"),"incorrect name format:" + name);
    }

    @PostMapping(value="/importAssignments", consumes = {"multipart/form-data"})
    @RolesAllowed({Role.ADMIN})
    public String importAssignments(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            validateImportedSmallFile(file);

            validateValidZipFileForLocation(file);
            validateValidAssignmentStorage(file.getOriginalFilename());
            File outputFile = new File(mojServerProperties.getAssignmentRepo().toFile().getParentFile(),file.getOriginalFilename());

            FileUtils.writeByteArrayToFile(outputFile, file.getBytes());
            log.info("unzipit quickly " +outputFile);
            ZipFileReader.unZipIt(outputFile.getPath());
            outputFile.delete();
            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded " + file.getOriginalFilename() + "!");
        } catch (Throwable ex) {
            log.error(ex.getMessage(),ex);
            redirectAttributes.addFlashAttribute("error",
                    "Upload failed!");
        }

        return "redirect:/control";
    }
}
