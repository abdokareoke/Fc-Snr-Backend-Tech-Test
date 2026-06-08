package com.flightcentre.scheduler.rules.impl;

import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.rules.BusinessRule;
import com.flightcentre.scheduler.rules.RuleContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * RULE-03: Weekly Hours Limit
 * An employee cannot be scheduled beyond their maximum weekly hours
 * within any 7-day rolling window (any continuous 168-hour period).
 *
 * The window is defined as the 168 hours leading up to and including the
 * candidate shift. Any existing shift overlapping this window is counted
 * in full — shifts straddling the boundary are not prorated. This is a
 * conservative overcount by design; it may flag a violation where precise
 * prorating would not, but avoids complexity for an edge case not present
 * in the current data set.
 *
 * The sliding window sum algorithm (O(n), prefix subtraction) was considered
 * for evaluating all possible 7-day windows across a wider date range, but
 * was not implemented because shifts span arbitrary time ranges rather than
 * discrete daily buckets, making bucketing a prerequisite. This remains a
 * valid pathway to solution if requirements evolve.
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
        Shift candidateShift = context.candidateShift();
        double maxWeeklyHours = context.employee().getMaxWeeklyHours();
        String employeeId = context.employee().getId();
        String employeeName = context.employee().getName();

        // rolling window: the 7 days leading up to and including the candidate shift
        LocalDateTime windowStart = candidateShift.getStartDatetime().minusHours(ROLLING_WINDOW_HOURS);
        LocalDateTime windowEnd = candidateShift.getEndDatetime();

        double hoursFromExisting = context.existingAssignments().stream()              // iterate employee's existing assignments
                .map(existing -> context.allShifts().get(existing.getShiftId()))       // resolve shiftId -> Shift object
                .filter(Objects::nonNull)                                               // skip if shift not found in map
                .filter(existingShift -> !existingShift.getId().equals(candidateShift.getId()))  // skip same shift (re-evaluation guard)
                .filter(existingShift -> existingShift.getStartDatetime().isBefore(windowEnd)
                        && existingShift.getEndDatetime().isAfter(windowStart))        // keep only shifts within the rolling window
                .mapToDouble(Shift::durationHours)                                     // extract duration in hours from each qualifying shift
                .sum();                                                                 // accumulate total hours

        double totalHours = candidateShift.durationHours() + hoursFromExisting;       // add candidate's hours to the rolling total

        if (totalHours > maxWeeklyHours) {
            return List.of(new Violation(
                    getRuleName(),
                    Violation.Severity.ERROR,
                    String.format("%s would exceed maximum weekly hours of %.0f (total: %.1f hours)",
                            employeeName, maxWeeklyHours, totalHours),
                    employeeId,
                    List.of(candidateShift.getId())
            ));
        }

        return List.of();                                                               // no violation
    }
}
