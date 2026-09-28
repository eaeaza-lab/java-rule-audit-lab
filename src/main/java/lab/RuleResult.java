package lab;

/** Value returned by a rule together with the trace entry for that rule firing. */
public record RuleResult<O>(O value, TraceEntry trace) {
    public RuleResult {
        if (value == null || trace == null) {
            throw new IllegalArgumentException("value and trace required");
        }
    }
}
