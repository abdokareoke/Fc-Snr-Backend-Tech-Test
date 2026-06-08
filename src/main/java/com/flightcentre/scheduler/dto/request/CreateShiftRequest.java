package com.flightcentre.scheduler.dto.request;

import com.flightcentre.scheduler.model.enums.Skill;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class CreateShiftRequest {

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Start datetime is required")
    private LocalDateTime startDatetime;

    @NotNull(message = "End datetime is required")
    private LocalDateTime endDatetime;

    @Min(value = 1, message = "Required headcount must be at least 1")
    private int requiredHeadcount;

    private List<Skill> requiredSkills;

    public String getLocation() { return location; }
    public LocalDateTime getStartDatetime() { return startDatetime; }
    public LocalDateTime getEndDatetime() { return endDatetime; }
    public int getRequiredHeadcount() { return requiredHeadcount; }
    public List<Skill> getRequiredSkills() { return requiredSkills; }

    public void setLocation(String location) { this.location = location; }
    public void setStartDatetime(LocalDateTime startDatetime) { this.startDatetime = startDatetime; }
    public void setEndDatetime(LocalDateTime endDatetime) { this.endDatetime = endDatetime; }
    public void setRequiredHeadcount(int requiredHeadcount) { this.requiredHeadcount = requiredHeadcount; }
    public void setRequiredSkills(List<Skill> requiredSkills) { this.requiredSkills = requiredSkills; }
}
