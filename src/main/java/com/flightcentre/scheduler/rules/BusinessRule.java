package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.valueobjects.Violation;
import java.util.List;

/**
 * Contract for all business rule validators.
 *
 * Each rule is a named, isolated, independently testable component.
 * Rules return a list of violations — empty means no violation found.
 *
 * Using a List allows a single rule to return multiple violations
 * (e.g. RULE-01 could find an employee overlapping with several shifts).
 */
public interface BusinessRule {

    /**
     * Evaluate this rule against the given context.
     *
     * @param context  All data needed to evaluate the rule
     * @return         List of violations found; empty list means the rule passes
     */
    List<Violation> evaluate(RuleContext context);

    /**
     * The rule's name identifier as it appears in violation responses.
     */
    String getRuleName();
}
