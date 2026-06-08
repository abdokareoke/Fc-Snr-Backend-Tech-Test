package com.flightcentre.scheduler.controller;

import com.flightcentre.scheduler.dto.request.CreateShiftRequest;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping
    public ResponseEntity<Shift> create(@Valid @RequestBody CreateShiftRequest request) {
        Shift shift = new Shift(
                null,
                request.getLocation(),
                request.getStartDatetime(),
                request.getEndDatetime(),
                request.getRequiredHeadcount(),
                request.getRequiredSkills() != null ? request.getRequiredSkills() : List.of()
        );

        Shift created = shiftService.create(shift);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shift> getById(@PathVariable String id) {
        return ResponseEntity.ok(shiftService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<Shift>> list(@RequestParam(required = false) String scheduleId) {
        if (scheduleId != null) {
            return ResponseEntity.ok(shiftService.getByScheduleId(scheduleId));
        }
        return ResponseEntity.ok(shiftService.getAll());
    }
}
