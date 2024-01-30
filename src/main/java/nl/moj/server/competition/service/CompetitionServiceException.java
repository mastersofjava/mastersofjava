package nl.moj.server.competition.service;

public class CompetitionServiceException extends Exception {

    public CompetitionServiceException(String message) {
        super(message);
    }

    public CompetitionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
