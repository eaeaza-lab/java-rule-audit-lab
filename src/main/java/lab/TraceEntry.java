package lab;

/** One rule firing: rule id and version, the inputs it saw, and the result it produced. */
public record TraceEntry(String ruleId, String version, String inputs, String result) {
    public TraceEntry {
        if (ruleId == null || ruleId.isEmpty() || version == null || version.isEmpty()) {
            throw new IllegalArgumentException("ruleId and version required");
        }
        if (inputs == null || result == null) {
            throw new IllegalArgumentException("inputs and result required");
        }
    }

    /** Single-line stable format: {@code rule=<id>@<version> inputs=[<inputs>] result=<result>}. */
    public String format() {
        return "rule=" + ruleId + "@" + version + " inputs=[" + inputs + "] result=" + result;
    }
}
