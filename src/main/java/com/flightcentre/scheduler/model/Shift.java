package com.flightcentre.scheduler.model;

import com.flightcentre.scheduler.model.enums.Skill;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a work shift that needs to be staffed.
 * Defines when, where, how many employees are needed, and what skills they must have.
 * The relationship to a Schedule is owned by Schedule via its shiftIds collection.
 */
public class Shift {

    private String id;
    private String location;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private int requiredHeadcount;
    private List<Skill> requiredSkills;

    public Shift() {}

    public Shift(String id, String location,
                 LocalDateTime startDatetime, LocalDateTime endDatetime,
                 int requiredHeadcount, List<Skill> requiredSkills) {
        this.id = id;
        this.location = location;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.requiredHeadcount = requiredHeadcount;
        this.requiredSkills = requiredSkills;
    }

    /**
     * Returns true if this shift's time window overlaps with another shift.
     * Used by RULE-01 (No Overlapping Shifts).
     * Two shifts overlap if one starts before the other ends.
     */
    public boolean overlapsWith(Shift other) {
        return this.startDatetime.isBefore(other.endDatetime)
                && other.startDatetime.isBefore(this.endDatetime);
    }

    /**
     * Returns the duration of this shift in hours.
     * Used by RULE-03 (Weekly Hours Limit).
     * assuming no partial shifts at this point
     */
    public double durationHours() {
        return java.time.Duration.between(startDatetime, endDatetime).toMinutes() / 60.0;
    }

    public String getId() { return id; }
    public String getLocation() { return location; }
    public LocalDateTime getStartDatetime() { return startDatetime; }
    public LocalDateTime getEndDatetime() { return endDatetime; }
    public int getRequiredHeadcount() { return requiredHeadcount; }
    public List<Skill> getRequiredSkills() { return requiredSkills; }

    public void setId(String id) { this.id = id; }
    public void setLocation(String location) { this.location = location; }
    public void setStartDatetime(LocalDateTime startDatetime) { this.startDatetime = startDatetime; }
    public void setEndDatetime(LocalDateTime endDatetime) { this.endDatetime = endDatetime; }
    public void setRequiredHeadcount(int requiredHeadcount) { this.requiredHeadcount = requiredHeadcount; }
    public void setRequiredSkills(List<Skill> requiredSkills) { this.requiredSkills = requiredSkills; }
}
