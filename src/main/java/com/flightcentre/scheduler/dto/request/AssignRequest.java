package com.flightcentre.scheduler.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AssignRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Shift ID is required")
    private String shiftId;

    public String getEmployeeId() { return employeeId; }
    public String getShiftId() { return shiftId; }

    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }
}
