package com.flightcentre.scheduler.model;

import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.model.enums.Skill;
import com.flightcentre.scheduler.model.valueobjects.AvailabilityWindow;

import java.util.List;

/**
 * Represents a workforce employee with their contract details,
 * skills, and weekly availability.
 */
public class Employee {

    private String id;
    private String name;
    private String email;
    private ContractType contractType;
    private double maxWeeklyHours;
    private List<Skill> skills;
    
    /**
     * Per-day availability windows (e.g. MON 06:00-22:00, TUE 06:00-22:00).
     * A List allows multiple windows per day if needed in the future,
     * and naturally handles employees with no availability on certain days
     * (simply no entry for that day).
     */
    private List<AvailabilityWindow> availabilityWindows;

    public Employee() {}

    public Employee(String id, String name, String email, ContractType contractType,
                    double maxWeeklyHours, List<Skill> skills,
                    List<AvailabilityWindow> availabilityWindows) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.contractType = contractType;
        this.maxWeeklyHours = maxWeeklyHours;
        this.skills = skills;
        this.availabilityWindows = availabilityWindows;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public ContractType getContractType() { return contractType; }
    public double getMaxWeeklyHours() { return maxWeeklyHours; }
    public List<Skill> getSkills() { return skills; }
    public List<AvailabilityWindow> getAvailabilityWindows() { return availabilityWindows; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setContractType(ContractType contractType) { this.contractType = contractType; }
    public void setMaxWeeklyHours(double maxWeeklyHours) { this.maxWeeklyHours = maxWeeklyHours; }
    public void setSkills(List<Skill> skills) { this.skills = skills; }
    public void setAvailabilityWindows(List<AvailabilityWindow> availabilityWindows) {
        this.availabilityWindows = availabilityWindows;
    }
}
