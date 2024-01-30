package nl.moj.worker.controller;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.assignment.service.AssignmentDescriptorService;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.common.util.ZipUtils;

@RequiredArgsConstructor
@Slf4j
public class RemoteControllerClient implements ControllerClient {

    private final Map<UUID, AssignmentDescriptor> cachedDescriptors = new ConcurrentHashMap<>();

    private final MojServerProperties mojServerProperties;

    private final AssignmentDescriptorService assignmentDescriptorService;

    private final RestTemplate restTemplate;

    public AssignmentDescriptor getAssignmentDescriptor(UUID assignmentUuid) throws IOException {
        if (!cachedDescriptors.containsKey(assignmentUuid)) {
            // download zip
            Path zipFile = fetchAssignmentZip(assignmentUuid);
            if (zipFile == null) {
                throw new IOException("Assignment " + assignmentUuid + " content not found.");
            }

            // extract to temp directory
            Path assignmentDir = Files.createTempDirectory("assignment");
            ZipUtils.unzip(zipFile, assignmentDir);

            // read descriptor
            AssignmentDescriptor descriptor = assignmentDescriptorService.findInFolder(assignmentDir);
            if (descriptor == null) {
                throw new IOException("Assignment " + assignmentUuid + " descriptor not found.");
            }

            // add to map
            cachedDescriptors.put(assignmentUuid, descriptor);

        }
        return cachedDescriptors.get(assignmentUuid);
    }

    private Path fetchAssignmentZip(UUID uuid) {
        return restTemplate.execute(resolveEndpoint(uuid), HttpMethod.GET, null, clientHttpResponse -> {
            if (clientHttpResponse.getStatusCode() == HttpStatus.OK) {
                Path dst = Files.createTempFile("assignment", "zip");
                StreamUtils.copy(clientHttpResponse.getBody(), Files.newOutputStream(dst, StandardOpenOption.WRITE));
                return dst;
            }
            return null;
        });
    }

    private URI resolveEndpoint(UUID uuid) {
        return mojServerProperties.getControllerEndpoint().resolve("/api/assignment/" + uuid + "/content");
    }

}
