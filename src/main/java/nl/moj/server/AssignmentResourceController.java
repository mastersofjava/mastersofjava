package nl.moj.server;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.authorization.Role;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.util.ZipUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AssignmentResourceController {

    private static final long MAXIMUM_UPLOAD_SIZE_IN_BYTES = 1000000L;

    private final MojServerProperties mojServerProperties;

    private final AssignmentService assignmentService;

    private void validateAssignmentsArchive(MultipartFile file) throws IOException {
        Assert.isTrue(file.getBytes().length > 0, "Empty assignments archive: " + file.getOriginalFilename() + ".");
        Assert.isTrue(file.getBytes().length < MAXIMUM_UPLOAD_SIZE_IN_BYTES, "Assignments archive exceeded maximum allowed size ");
        Assert.isTrue(isZip(file), "Assignments archive should be a zip file.");
    }

    private boolean isZip(MultipartFile file) throws IOException {
        try {
            new ZipInputStream(file.getInputStream()).getNextEntry();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @PostMapping(value = "/importAssignments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public ResponseEntity<Map<String, String>> importAssignments(@RequestParam("file") MultipartFile file, @RequestParam("collection") String collection) {
        Map<String, String> results = new HashMap<>();
        if (file == null) {
            results.put("message", "Assignment archive upload failed, no archive found.");
            return ResponseEntity.badRequest().body(results);
        }

        try {
            Path dest = mojServerProperties.getAssignmentRepo().resolve(collection);
            boolean destExistedAlready = Files.exists(dest);

            validateAssignmentsArchive(file);
            Files.createDirectories(dest);
            ZipUtils.unzip(file.getResource().getFile().toPath(), dest);

            results.put("message", String.format("Assignment archive uploaded and assignments scanned. Assignments added to the collection %s.", collection));
            if (destExistedAlready) {
                results.put("message", results.get("message") + " The collection existed already, assignments are updated based on archive contents.");
            }

            return ResponseEntity.ok(results);

        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            results.put("message", String.format("Assignment archive upload failed. %s", ex.getMessage()));
            return ResponseEntity.badRequest().body(results);
        }
    }

    // TODO resources might need to be declared in the assignment descriptor.
    // In theory it would now be possible to read any assignment file except for solutions as they
    // are explicitly excluded.

    /**
     * this method ensures that one can place images in the assets of an assignment.
     * sample usage would be: <img src='/public/assignment_image/<assignment-uuid>/public_Hexagonal_chess.svg'>
     *
     * @param assignment - the assignment name
     * @param file       - the file name of the image that should be rendered
     */
    @GetMapping("/public/asset/{assignment}/{*file}")
    @ResponseBody
    public ResponseEntity<FileSystemResource> insertAssignmentImage(@PathVariable("assignment") UUID assignment, @PathVariable("file") String file) {
        try {
            // check it is not the solution that is requested.
            AssignmentDescriptor ad = assignmentService.resolveAssignmentDescriptor(assignment);
            if (ad.getAssignmentFiles().getSolution().stream().anyMatch(s -> Paths.get(file).normalize().equals(s))) {
                return ResponseEntity.notFound().build();
            }

            // multi path segment matching always gives a / at the start, strip it.
            String relFile = file.substring(1);

            // make sure we are not reading outside the assignment content folder, just in case the strict firewall
            // does not filter the request
            Path assignmentBase = assignmentService.getAssignmentContentFolder(assignment);
            Path resource = assignmentBase.resolve(relFile).normalize();
            if (!resource.startsWith(assignmentBase)) {
                return ResponseEntity.notFound().build();
            }
            FileSystemResource fsr = new FileSystemResource(resource);
            String mimeType = MimeMappings.DEFAULT.get(FilenameUtils.getExtension(resource.getFileName().toString()));
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(mimeType)).contentLength(fsr.contentLength()).body(fsr);
        } catch (Exception e) {
            log.error("Unexpected exception reading assignment resource {} for assignment {}.", file, assignment, e);
            return ResponseEntity.notFound().build();
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