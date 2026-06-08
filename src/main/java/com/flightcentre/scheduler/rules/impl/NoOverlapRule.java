package com.flightcentre.scheduler.rules.impl;

import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.rules.BusinessRule;
import com.flightcentre.scheduler.rules.RuleContext;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;

/**
 * RULE-01: No Overlapping Shifts
 * An employee cannot be assigned to two shifts whose times overlap.
 */
@Component
public class NoOverlapRule implements BusinessRule {

    @Override
    public String getRuleName() {
        return "OVERLAP";
    }

    @Override
    public List<Violation> evaluate(RuleContext context) {
        Shift candidateShift = context.candidateShift();
        String employeeName = context.employee().getName();
        String employeeId = context.employee().getId();

        List<Violation> violations = context.existingAssignments().stream()   // iterate employee's existing assignments
                .map(existing -> context.allShifts().get(existing.getShiftId()))  // resolve shiftId -> Shift object
                .filter(Objects::nonNull)                                          // skip if shift not found in map
                .filter(existingShift -> !existingShift.getId().equals(candidateShift.getId()))  // skip same shift (re-evaluation guard)
                .filter(existingShift -> candidateShift.overlapsWith(existingShift))    // keep only shifts that overlap
                .map(existingShift -> new Violation(                               // build a violation for each overlap found
                        getRuleName(),
                        Violation.Severity.ERROR,
                        String.format("%s is assigned to overlapping shifts on %s",
                                employeeName,
                                candidateShift.getStartDatetime().toLocalDate()),
                        employeeId,
                        List.of(candidateShift.getId(), existingShift.getId())
                ))
                .toList();                                                          // collect results into an immutable list

        return violations;
    }
}
