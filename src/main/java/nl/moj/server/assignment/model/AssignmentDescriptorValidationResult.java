package nl.moj.server.assignment.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;

import java.util.ArrayList;
import java.util.List;


@Getter
@AllArgsConstructor
public class AssignmentDescriptorValidationResult {

	private final String assignment;
	private final AssignmentDescriptor assignmentDescriptor;
	private final List<String> validationMessages = new ArrayList<>();

	public void addValidationMessage(String message) {
		validationMessages.add(message);
	}

	public boolean isValid() {
		return assignmentDescriptor != null && validationMessages.isEmpty();
	}
}
