package com.flightcentre.scheduler.model.valueobjects;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The full conflict report returned by GET /schedules/{id}/conflicts.
 * Contains all rule violations found across the schedule.
 */
public class ConflictReport {

    private String scheduleId;
    private LocalDateTime evaluatedAt;
    private int totalViolations;
    private List<Violation> violations;

    public ConflictReport() {}

    public ConflictReport(String scheduleId, LocalDateTime evaluatedAt, List<Violation> violations) {
        this.scheduleId = scheduleId;
        this.evaluatedAt = evaluatedAt;
        this.violations = violations;
        this.totalViolations = violations.size();
    }

    public String getScheduleId() { return scheduleId; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public int getTotalViolations() { return totalViolations; }
    public List<Violation> getViolations() { return violations; }

    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public void setTotalViolations(int totalViolations) { this.totalViolations = totalViolations; }
    public void setViolations(List<Violation> violations) { this.violations = violations; }
}
