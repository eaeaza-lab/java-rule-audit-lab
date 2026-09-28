package lab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Deterministic registry of rule implementations, addressed by id and version. */
public final class RuleRegistry {
    private final Map<String, Map<String, Rule<?, ?>>> rulesById = new LinkedHashMap<>();

    /** Registers a rule. A duplicate id/version pair is rejected. */
    public void register(Rule<?, ?> rule) {
        if (rule == null) {
            throw new IllegalArgumentException("rule required");
        }
        Map<String, Rule<?, ?>> versions = rulesById.computeIfAbsent(rule.id(), ignored -> new LinkedHashMap<>());
        if (versions.putIfAbsent(rule.version(), rule) != null) {
            throw new IllegalArgumentException("duplicate rule " + rule.id() + "@" + rule.version());
        }
    }

    /** Returns the requested rule version, when it is registered. */
    public Optional<Rule<?, ?>> find(String id, String version) {
        Map<String, Rule<?, ?>> versions = rulesById.get(id);
        return versions == null ? Optional.empty() : Optional.ofNullable(versions.get(version));
    }

    /** Registered rules in their stable registration order. */
    public List<Rule<?, ?>> all() {
        List<Rule<?, ?>> result = new ArrayList<>();
        for (Map<String, Rule<?, ?>> versions : rulesById.values()) {
            result.addAll(versions.values());
        }
        return List.copyOf(result);
    }

    /** Registry used by the offline rule engine at this stage. */
    public static RuleRegistry standard() {
        RuleRegistry registry = new RuleRegistry();
        registry.register(TaxRule.INSTANCE);
        return registry;
    }
}
