package nl.moj.common.assignment.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;

@Service
@Slf4j
public class AssignmentDescriptorService {

    private final ObjectMapper yamlObjectMapper;

    public AssignmentDescriptorService(@Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper) {
        this.yamlObjectMapper = yamlObjectMapper;
    }

    public AssignmentDescriptor parseAssignmentDescriptor(Path descriptor) {
        AssignmentDescriptor ad;
        try {
            ad = yamlObjectMapper.readValue(Files.newInputStream(descriptor),
                    AssignmentDescriptor.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ad.setDirectory(descriptor.getParent());
        return ad;
    }

    public AssignmentDescriptor findInFolder(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            List<Path> descriptors = files.filter(this::isAssignmentDescriptor).toList();
            if (descriptors.size() == 1) {
                return parseAssignmentDescriptor(descriptors.get(0));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    public boolean isAssignmentDescriptor(Path f) {
        return f.getFileName().toString().equals("assignment.yaml")
                || f.getFileName().toString().equals("assignment.yml");
    }
}
