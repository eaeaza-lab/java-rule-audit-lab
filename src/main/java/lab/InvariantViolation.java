package lab;

/** A stable, human-readable explanation of a failed post-rule invariant. */
public record InvariantViolation(String code, String detail) {
    public InvariantViolation {
        if (code == null || code.isEmpty() || detail == null || detail.isEmpty()) {
            throw new IllegalArgumentException("code and detail required");
        }
    }

    /** Formats the violation for a CLI audit result. */
    public String format() {
        return code + ": " + detail;
    }
}
