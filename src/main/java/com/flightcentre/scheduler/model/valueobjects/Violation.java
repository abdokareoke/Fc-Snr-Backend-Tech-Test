package com.flightcentre.scheduler.model.valueobjects;

import java.util.List;

/**
 * Represents a single business rule violation detected in a schedule.
 * Returned as part of a ConflictReport from GET /schedules/{id}/conflicts.
 */
public class Violation {

    public enum Severity {
        ERROR,   // Hard violation — must be resolved (overlap, rest, skill, availability)
        WARNING  // Coverage concern — should be reviewed (example: understaffed)
    }

    private String rule;
    private Severity severity;
    private String message;
    private String affectedEmployeeId;
    private List<String> affectedShiftIds;

    public Violation() {}

    public Violation(String rule, Severity severity, String message,
                     String affectedEmployeeId, List<String> affectedShiftIds) {
        this.rule = rule;
        this.severity = severity;
        this.message = message;
        this.affectedEmployeeId = affectedEmployeeId;
        this.affectedShiftIds = affectedShiftIds;
    }

    public String getRule() { return rule; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getAffectedEmployeeId() { return affectedEmployeeId; }
    public List<String> getAffectedShiftIds() { return affectedShiftIds; }

    public void setRule(String rule) { this.rule = rule; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public void setMessage(String message) { this.message = message; }
    public void setAffectedEmployeeId(String affectedEmployeeId) { this.affectedEmployeeId = affectedEmployeeId; }
    public void setAffectedShiftIds(List<String> affectedShiftIds) { this.affectedShiftIds = affectedShiftIds; }
}
