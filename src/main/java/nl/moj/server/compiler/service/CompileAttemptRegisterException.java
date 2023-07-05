package nl.moj.server.compiler.service;

public class CompileAttemptRegisterException extends Exception {
    
    public CompileAttemptRegisterException(String message) {
        super(message);
    }

    public CompileAttemptRegisterException(String message, Throwable cause) {
        super(message, cause);
    }
}
