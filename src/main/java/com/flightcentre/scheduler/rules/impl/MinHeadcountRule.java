package com.flightcentre.scheduler.rules.impl;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.rules.BusinessRule;
import com.flightcentre.scheduler.rules.RuleContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RULE-05: Minimum Headcount
 * A shift must have at least the required number of employees assigned
 * to be considered fully covered.
 *
 * Severity: WARNING (coverage concern, not a hard violation).
 * affectedEmployeeId is not set — this is a shift-level concern, not employee-level.
 */
@Component
public class MinHeadcountRule implements BusinessRule {

    @Override
    public String getRuleName() {
        return "UNDERSTAFFED";
    }

    @Override
    public List<Violation> evaluate(RuleContext context) {
        String shiftId = context.candidateShift().getId();
        int required = context.candidateShift().getRequiredHeadcount();

        // Count assignments for this shift including the candidate
        int assignedCount = context.assignmentsByShift()
                .getOrDefault(shiftId, List.of())
                .size() + 1;

        if (assignedCount < required) {
            return List.of(new Violation(
                    getRuleName(),
                    Violation.Severity.WARNING,
                    String.format("Shift %s requires %d employees but only %d are assigned",
                            shiftId, required, assignedCount),
                    null,  // No specific employee — this is a shift-level concern
                    List.of(shiftId)
            ));
        }

        return List.of();
    }
}
