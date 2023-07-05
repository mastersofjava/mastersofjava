package nl.moj.server.test.service;

public class TestAttemptRegisterException extends Exception {

    public TestAttemptRegisterException(String message) {
        super(message);
    }

    public TestAttemptRegisterException(String message, Throwable cause) {
        super(message, cause);
    }
}
