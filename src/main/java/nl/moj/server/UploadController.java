package nl.moj.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.teams.model.Role;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.security.RolesAllowed;
import java.util.Arrays;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UploadController {

    private final CompetitionService competitionService;


    private void validateImportedCsv(MultipartFile file) {
        Assert.isTrue(file.getSize()==0,"empty input given");
        Assert.isTrue(file.getSize()>1000*1000,"max size exceeded");
    }

    @PostMapping("/importUsers")
    @RolesAllowed({Role.ADMIN})
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            validateImportedCsv(file);
            String[] lines = new String(file.getBytes()).split("\n");
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

}
