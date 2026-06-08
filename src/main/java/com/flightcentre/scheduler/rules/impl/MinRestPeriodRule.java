package com.flightcentre.scheduler.rules.impl;

import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.rules.BusinessRule;
import com.flightcentre.scheduler.rules.RuleContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * RULE-02: Minimum Rest Period
 * An employee must have at least 11 hours of rest between
 * the end of one shift and the start of the next.
 */
@Component
public class MinRestPeriodRule implements BusinessRule {

    private static final long MINIMUM_REST_HOURS = 11;

    @Override
    public String getRuleName() {
        return "REST_VIOLATION";
    }

    @Override
    public List<Violation> evaluate(RuleContext context) {
        Shift candidateShift = context.candidateShift();
        String employeeName = context.employee().getName();
        String employeeId = context.employee().getId();

        List<Violation> violations = context.existingAssignments().stream()           // iterate employee's existing assignments
                .map(existing -> context.allShifts().get(existing.getShiftId()))      // resolve shiftId -> Shift object
                .filter(Objects::nonNull)                                              // skip if shift not found in map
                .filter(existingShift -> !existingShift.getId().equals(candidateShift.getId()))  // skip same shift (re-evaluation guard)
                .filter(existingShift -> {
                    long restHours = getRestHoursBetween(candidateShift, existingShift);
                    return restHours >= 0 && restHours < MINIMUM_REST_HOURS;  // skip overlaps (-1) and sufficient rest gaps
                })
                .map(existingShift -> {
                    // determine chronological order to name the "later" shift in the message
                    Shift earlierShift = candidateShift.getEndDatetime().isBefore(existingShift.getStartDatetime())
                            ? candidateShift : existingShift;
                    Shift laterShift = earlierShift == candidateShift ? existingShift : candidateShift;

                    return new Violation(                                              // build violation with both shift ids
                            getRuleName(),
                            Violation.Severity.ERROR,
                            String.format("%s has less than %d hours rest before shift %s",
                                    employeeName, MINIMUM_REST_HOURS, laterShift.getId()),
                            employeeId,
                            List.of(earlierShift.getId(), laterShift.getId())
                    );
                })
                .toList();                                                             // collect results into an immutable list

        return violations;
    }

    /**
     * Returns the rest hours between two non-overlapping shifts.
     * Returns -1 if the shifts overlap (RULE-01 handles that case).
     */
    private long getRestHoursBetween(Shift a, Shift b) {
        if (a.overlapsWith(b)) return -1;

        Duration gap = a.getEndDatetime().isBefore(b.getStartDatetime())
                ? Duration.between(a.getEndDatetime(), b.getStartDatetime())
                : Duration.between(b.getEndDatetime(), a.getStartDatetime());

        return gap.toHours();
    }
}
