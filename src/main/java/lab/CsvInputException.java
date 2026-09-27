package lab;

/** Describes a malformed scenario input, including the source file and line when available. */
public final class CsvInputException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public CsvInputException(String source, int lineNumber, String message) {
        super(source + ":" + lineNumber + ": " + message);
    }

    public CsvInputException(String source, String message) {
        super(source + ": " + message);
    }
}
