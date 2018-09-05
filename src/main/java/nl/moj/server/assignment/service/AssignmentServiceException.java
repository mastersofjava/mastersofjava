package nl.moj.server.assignment.service;

/**
 * @author Ejnar Kaekebeke
 */
public class AssignmentServiceException extends RuntimeException {
    public AssignmentServiceException(String message) {
        super(message);
    }

    public AssignmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
