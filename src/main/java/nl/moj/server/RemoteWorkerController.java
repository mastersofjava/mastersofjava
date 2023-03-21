package nl.moj.server;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.common.util.ZipUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RemoteWorkerController {

    private final AssignmentService assignmentService;

    @GetMapping(value = "/api/assignment/{id}/content", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getAssignmentContent(@PathVariable("id") UUID uuid, HttpServletResponse response) throws IOException {
        AssignmentDescriptor ad = assignmentService.resolveAssignmentDescriptor(uuid);
        Path src = assignmentService.getAssignmentContentFolder(uuid);

        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"assignment-" + uuid + ".zip\"");

        try (OutputStream out = response.getOutputStream()) {
            ZipUtils.zip(src, out, path -> {
                boolean accept = ad.getAssignmentFiles().getSolution().stream().noneMatch(path::endsWith);
                log.info("Path {} accepted -> {}", path, accept);
                return accept;
            });
        }
    }
}
