package nl.moj.server.compile;

import static java.util.Objects.requireNonNull;

public final class DiagnosticDTO {

    private final String fileName;
    private final long lineNumber;
    private final String message;

    private DiagnosticDTO(String fileName, long lineNumber, String message) {
        this.fileName = requireNonNull(fileName);
        this.lineNumber = lineNumber;
        this.message = requireNonNull(message);
    }

    public static DiagnosticDTO of(String fileName, long lineNumber, String message) {
        return new DiagnosticDTO(fileName, lineNumber, message);
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiagnosticDTO that = (DiagnosticDTO) o;

        if (lineNumber != that.lineNumber) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return fileName != null ? fileName.equals(that.fileName) : that.fileName == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (lineNumber ^ (lineNumber >>> 32));
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s:%d: %s",
                fileName,
                lineNumber,
                message);
    }
}
