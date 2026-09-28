package lab;

/** A versioned business rule that produces both a value and its decision trace. */
public interface Rule<I, O> {
    /** Stable identifier for the kind of business rule. */
    String id();

    /** Version of the behavior implemented by this rule. */
    String version();

    /** Evaluates one input and records the inputs and result in the returned trace. */
    RuleResult<O> evaluate(I input);
}
