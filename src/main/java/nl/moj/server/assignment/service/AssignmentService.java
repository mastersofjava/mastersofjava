package nl.moj.server.assignment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentService {

	@Qualifier("yamlObjectMapper")
	private final ObjectMapper yamlObjectMapper;

	private final AssignmentRepository assignmentRepository;

	public AssignmentDescriptor getAssignmentDescriptor(Assignment assignment) {
		try {
			Path descriptor = Paths.get(assignment.getAssignmentDescriptor());
			AssignmentDescriptor assignmentDescriptor = yamlObjectMapper.readValue(Files.newInputStream(descriptor), AssignmentDescriptor.class);
			assignmentDescriptor.setDirectory(descriptor.getParent());
			return assignmentDescriptor;
		} catch( Exception e ) {
			throw new AssignmentServiceException("Unable to read assignment descriptor "+assignment.getAssignmentDescriptor()+".",e);
		}
	}

	public List<Assignment> updateAssignments(Path base) {

		log.info("Discovering assignments from {}.", base);

		return findAssignments(base).stream().map( d -> {
			Assignment current = assignmentRepository.findByName(d.getName());
			if( current != null ) {
				log.info("Updating existing assignment {}.", current.getName());
				current.setAssignmentDescriptor(d.getAssignmentDescriptor());
				return assignmentRepository.save(current);
			} else {
				log.info("Added new assignment {}.", d.getName());
				Assignment a = new Assignment();
				a.setAssignmentDescriptor(d.getAssignmentDescriptor());
				a.setName(d.getName());
				a.setUuid(UUID.randomUUID());
				return assignmentRepository.save(a);
			}
		}).collect(Collectors.toList());
	}

	private List<Assignment> findAssignments(Path base) {
		List<Assignment> result = new ArrayList<>();

		try {
			Files.walk(base, 1).forEach(path -> {
				try {
					Files.walk(path, 1).filter(file -> file.getFileName().toString().equals("assignment.yaml")).forEach(file -> {
						try {
							AssignmentDescriptor assignmentDescriptor = yamlObjectMapper.readValue(file.toFile(), AssignmentDescriptor.class);
							Assignment assignment = new Assignment();
							assignment.setName(assignmentDescriptor.getName());
							assignment.setAssignmentDescriptor(file.toAbsolutePath().toString());
							result.add(assignment);
						} catch (Exception e) {
							throw new RuntimeException("Unable to parse assignment descriptor " + file.toString(), e);
						}
					});
				} catch (Exception e) {
					throw new RuntimeException("Unable to find assignments in " + path, e);
				}
			});
		} catch (Exception e) {
			throw new AssignmentServiceException("Failed to read assignments from " + base, e);
		}
		return result;
	}
}
