package com.flightcentre.scheduler.model.valueobjects;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Represents an employee's availability for a specific day of the week.
 * e.g. MONDAY 06:00 - 22:00
 *
 * Using LocalTime (not String) so RULE-06 can do proper time comparisons
 * without parsing strings during every validation call.
 */
public class AvailabilityWindow {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public AvailabilityWindow() {}

    public AvailabilityWindow(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Returns true if the given time range falls entirely within this window.
     * Used by RULE-06 (Availability Matching).
     */
    public boolean covers(LocalTime shiftStart, LocalTime shiftEnd) {
        return !shiftStart.isBefore(startTime) && !shiftEnd.isAfter(endTime);
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
