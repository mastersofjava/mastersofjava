package nl.moj.worker.controller;

import java.io.IOException;
import java.util.UUID;

import nl.moj.common.assignment.descriptor.AssignmentDescriptor;

public interface ControllerClient {
    AssignmentDescriptor getAssignmentDescriptor(UUID assignmentUuid) throws IOException;
}
