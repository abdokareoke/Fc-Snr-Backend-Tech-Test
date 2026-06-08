package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.valueobjects.Violation;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Orchestrates all registered business rules.
 *
 * Spring automatically injects all BusinessRule implementations
 * via the List<BusinessRule> constructor parameter.
 * Adding a new rule is as simple as creating a new @Component
 * that implements BusinessRule — no changes needed here.
 */
@Component
public class RuleEvaluator {

    private final List<BusinessRule> rules;

    public RuleEvaluator(List<BusinessRule> rules) {
        this.rules = rules;
    }

    /**
     * Run all rules against the given context.
     * Returns all violations found across all rules.
     */
    public List<Violation> evaluate(RuleContext context) {
        return rules.stream()
                .flatMap(rule -> rule.evaluate(context).stream())
                .toList();
    }

    /**
     * Run all rules and return true if there are any ERROR-level violations.
     * Used by the assignment endpoint to block invalid assignments.
     * WARNING-level violations (e.g. understaffed) do not block assignment.
     */
    public boolean hasBlockingViolations(RuleContext context) {
        return evaluate(context).stream()
                .anyMatch(v -> v.getSeverity() == Violation.Severity.ERROR);
    }
}
