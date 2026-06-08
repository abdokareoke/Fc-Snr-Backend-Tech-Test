package com.flightcentre.scheduler.controller;

import com.flightcentre.scheduler.dto.request.CreateEmployeeRequest;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.AvailabilityWindow;
import com.flightcentre.scheduler.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<Employee> create(@Valid @RequestBody CreateEmployeeRequest request) {
        Employee employee = new Employee(
                null,
                request.getName(),
                request.getEmail(),
                request.getContractType(),
                request.getMaxWeeklyHours(),
                request.getSkills() != null ? request.getSkills() : List.of(),
                request.getAvailabilityWindows() != null ? request.getAvailabilityWindows() : List.of()
        );

        Employee created = employeeService.create(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getById(@PathVariable String id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<Shift>> getSchedule(
            @PathVariable String id,
            @RequestParam(required = false) String week) {
        return ResponseEntity.ok(employeeService.getScheduleForEmployee(id, week));
    }
}
