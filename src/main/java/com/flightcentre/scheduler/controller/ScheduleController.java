package com.flightcentre.scheduler.controller;

import com.flightcentre.scheduler.dto.request.AssignRequest;
import com.flightcentre.scheduler.dto.request.CreateScheduleRequest;
import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Schedule;
import com.flightcentre.scheduler.model.valueobjects.ConflictReport;
import com.flightcentre.scheduler.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<Schedule> create(@Valid @RequestBody CreateScheduleRequest request) {
        Schedule schedule = new Schedule(
                null,
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        );

        // Add shift IDs if provided
        if (request.getShiftIds() != null) {
            request.getShiftIds().forEach(schedule::addShiftId);
        }

        Schedule created = scheduleService.create(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getById(@PathVariable String id) {
        return ResponseEntity.ok(scheduleService.getById(id));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<Assignment> assign(
            @PathVariable String id,
            @Valid @RequestBody AssignRequest request) {
        Assignment assignment = scheduleService.assignEmployeeToShift(
                id, request.getEmployeeId(), request.getShiftId());
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @DeleteMapping("/{id}/assign")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable String id,
            @RequestParam String employeeId,
            @RequestParam String shiftId) {
        scheduleService.removeAssignment(id, employeeId, shiftId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/conflicts")
    public ResponseEntity<ConflictReport> getConflicts(@PathVariable String id) {
        return ResponseEntity.ok(scheduleService.detectConflicts(id));
    }

    @PostMapping("/{id}/auto-assign")
    public ResponseEntity<Schedule> autoAssign(@PathVariable String id) {
        Schedule schedule = scheduleService.autoAssign(id);
        return ResponseEntity.ok(schedule);
    }
}
