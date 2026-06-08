package com.flightcentre.scheduler.service;

import com.flightcentre.scheduler.exception.ResourceNotFoundException;
import com.flightcentre.scheduler.model.Schedule;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.repository.ScheduleRepository;
import com.flightcentre.scheduler.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for Shift operations.
 */
@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final ScheduleRepository scheduleRepository;

    public ShiftService(ShiftRepository shiftRepository, ScheduleRepository scheduleRepository) {
        this.shiftRepository = shiftRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Shift create(Shift shift) {
        if (shift.getId() == null || shift.getId().isBlank()) {
            shift.setId("shf-" + UUID.randomUUID().toString().substring(0, 8));
        }

        return shiftRepository.save(shift);
    }

    public Shift getById(String id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id));
    }

    /**
     * GET /shifts?scheduleId={id}
     * Returns all shifts belonging to a schedule.
     */
    public List<Shift> getByScheduleId(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

        return shiftRepository.findAllById(schedule.getShiftIds());
    }

    public List<Shift> getAll() {
        return shiftRepository.findAll();
    }
}
