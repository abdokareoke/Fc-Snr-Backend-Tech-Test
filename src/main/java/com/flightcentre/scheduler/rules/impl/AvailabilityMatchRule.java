package com.flightcentre.scheduler.rules.impl;

import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.valueobjects.AvailabilityWindow;
import com.flightcentre.scheduler.rules.BusinessRule;
import com.flightcentre.scheduler.rules.RuleContext;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * RULE-06: Availability Matching
 * An employee can only be assigned to a shift if the shift's time window
 * falls entirely within the employee's availability windows for that day.
 *
 * Checks both the start day and end day of the shift — a shift that spans
 * midnight (e.g. Tue 22:00 - Wed 06:00) must be valid for both days.
 */
@Component
public class AvailabilityMatchRule implements BusinessRule {

    @Override
    public String getRuleName() {
        return "AVAILABILITY_VIOLATION";
    }

    @Override
    public List<Violation> evaluate(RuleContext context) {
        Shift shift = context.candidateShift();
        List<AvailabilityWindow> windows = context.employee().getAvailabilityWindows();

        if (windows == null || windows.isEmpty()) {
            return List.of(new Violation(
                    getRuleName(),
                    Violation.Severity.ERROR,
                    String.format("%s has no availability windows defined",
                            context.employee().getName()),
                    context.employee().getId(),
                    List.of(shift.getId())
            ));
        }

        DayOfWeek shiftDay = shift.getStartDatetime().getDayOfWeek();
        LocalTime shiftStart = shift.getStartDatetime().toLocalTime();
        LocalTime shiftEnd = shift.getEndDatetime().toLocalTime();

        boolean shiftSpansMidnight = shift.getEndDatetime().toLocalDate()
                .isAfter(shift.getStartDatetime().toLocalDate());

        if (shiftSpansMidnight) {
            // For overnight shifts: check start day covers start→midnight
            // and end day covers midnight→end
            boolean startDayCovered = isAvailableFor(windows, shiftDay, shiftStart, LocalTime.MAX);
            DayOfWeek endDay = shift.getEndDatetime().getDayOfWeek();
            boolean endDayCovered = isAvailableFor(windows, endDay, LocalTime.MIDNIGHT, shiftEnd);

            if (!startDayCovered || !endDayCovered) {
                return buildViolation(context, shift);
            }
        } else {
            if (!isAvailableFor(windows, shiftDay, shiftStart, shiftEnd)) {
                return buildViolation(context, shift);
            }
        }

        return List.of();
    }

    private boolean isAvailableFor(List<AvailabilityWindow> windows,
                                    DayOfWeek day, LocalTime start, LocalTime end) {
        return windows.stream()
                .filter(w -> w.getDayOfWeek() == day)
                .anyMatch(w -> w.covers(start, end));
    }

    private List<Violation> buildViolation(RuleContext context, Shift shift) {
        return List.of(new Violation(
                getRuleName(),
                Violation.Severity.ERROR,
                String.format("%s is not available for shift %s on %s",
                        context.employee().getName(),
                        shift.getId(),
                        shift.getStartDatetime().toLocalDate()),
                context.employee().getId(),
                List.of(shift.getId())
        ));
    }
}
