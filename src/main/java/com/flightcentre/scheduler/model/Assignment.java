package com.flightcentre.scheduler.model;

/**
 * Represents the link between an employee and a shift within a schedule.
 * This is the join entity — it answers "who is working which shift".
 *
 * The composite of (scheduleId, shiftId, employeeId) is naturally unique,
 * but we carry an explicit id for simpler lookup and deletion.
 */
public class Assignment {

    private String id;
    private String scheduleId;
    private String shiftId;
    private String employeeId;

    public Assignment() {}

    public Assignment(String id, String scheduleId, String shiftId, String employeeId) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.shiftId = shiftId;
        this.employeeId = employeeId;
    }

    public String getId() { return id; }
    public String getScheduleId() { return scheduleId; }
    public String getShiftId() { return shiftId; }
    public String getEmployeeId() { return employeeId; }

    public void setId(String id) { this.id = id; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
}
