package com.flightcentre.scheduler.rules.impl;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.rules.BusinessRule;
import com.flightcentre.scheduler.rules.RuleContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RULE-03: Weekly Hours Limit
 * An employee cannot be scheduled beyond their maximum weekly hours
 * within any 7-day rolling window (any continuous 168-hour period).
 *
 * "Rolling window" means: for the candidate shift, look at all shifts
 * that overlap with the 7-day window centred around the candidate.
 * This is stricter than a calendar week check.
 */
@Component
public class WeeklyHoursLimitRule implements BusinessRule {

    private static final long ROLLING_WINDOW_HOURS = 168; // 7 days

    @Override
    public String getRuleName() {
        return "WEEKLY_HOURS_EXCEEDED";
    }

    @Override
    public List<Violation> evaluate(RuleContext context) {
        List<Violation> violations = new ArrayList<>();

        Shift candidate = context.candidateShift();
        double maxWeeklyHours = context.employee().getMaxWeeklyHours();
        String employeeId = context.employee().getId();
        String employeeName = context.employee().getName();

        // The rolling window: 7 days before the candidate shift starts
        // to 7 days after it ends — we find all shifts within this window
        // and sum their hours including the candidate.
        LocalDateTime windowStart = candidate.getStartDatetime().minusHours(ROLLING_WINDOW_HOURS);
        LocalDateTime windowEnd = candidate.getEndDatetime().plusHours(ROLLING_WINDOW_HOURS);

        double totalHours = candidate.durationHours();

        for (Assignment existing : context.existingAssignments()) {
            Shift existingShift = context.allShifts().get(existing.getShiftId());
            if (existingShift == null) continue;
            if (existingShift.getId().equals(candidate.getId())) continue;

            // Include shift if it falls within the 168-hour rolling window
            if (existingShift.getStartDatetime().isBefore(windowEnd)
                    && existingShift.getEndDatetime().isAfter(windowStart)) {
                totalHours += existingShift.durationHours();
            }
        }

        if (totalHours > maxWeeklyHours) {
            violations.add(new Violation(
                    getRuleName(),
                    Violation.Severity.ERROR,
                    String.format("%s would exceed maximum weekly hours of %.0f (total: %.1f hours)",
                            employeeName, maxWeeklyHours, totalHours),
                    employeeId,
                    List.of(candidate.getId())
            ));
        }

        return violations;
    }
}
