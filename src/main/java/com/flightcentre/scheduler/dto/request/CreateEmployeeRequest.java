package com.flightcentre.scheduler.dto.request;

import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.model.enums.Skill;
import com.flightcentre.scheduler.model.valueobjects.AvailabilityWindow;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CreateEmployeeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Contract type is required")
    private ContractType contractType;

    @Positive(message = "Max weekly hours must be positive")
    private double maxWeeklyHours;

    private List<Skill> skills;
    private List<AvailabilityWindow> availabilityWindows;

    public String getName() { return name; }
    public String getEmail() { return email; }
    public ContractType getContractType() { return contractType; }
    public double getMaxWeeklyHours() { return maxWeeklyHours; }
    public List<Skill> getSkills() { return skills; }
    public List<AvailabilityWindow> getAvailabilityWindows() { return availabilityWindows; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setContractType(ContractType contractType) { this.contractType = contractType; }
    public void setMaxWeeklyHours(double maxWeeklyHours) { this.maxWeeklyHours = maxWeeklyHours; }
    public void setSkills(List<Skill> skills) { this.skills = skills; }
    public void setAvailabilityWindows(List<AvailabilityWindow> availabilityWindows) { this.availabilityWindows = availabilityWindows; }
}
