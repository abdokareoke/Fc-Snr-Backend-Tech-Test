package com.flightcentre.scheduler.repository;

import com.flightcentre.scheduler.model.Assignment;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Assignment persistence.
 *
 * Assignments are the most queried entity in the rule engine —
 * we need fast lookup by employee and by schedule/shift.
 * The implementation maintains indexes for both access patterns.
 */
public interface AssignmentRepository {

    Assignment save(Assignment assignment);

    Optional<Assignment> findById(String id);

    /**
     * Find all assignments for a given employee.
     * Used by RULE-01 (overlap), RULE-02 (rest), RULE-03 (weekly hours).
     */
    List<Assignment> findByEmployeeId(String employeeId);

    /**
     * Find all assignments within a schedule.
     * Used by conflict detection across the whole schedule.
     */
    List<Assignment> findByScheduleId(String scheduleId);

    /**
     * Find all assignments for a specific shift.
     * Used by RULE-05 (headcount check).
     */
    List<Assignment> findByShiftId(String shiftId);

    /**
     * Find a specific employee-shift assignment within a schedule.
     * Used to prevent duplicate assignments.
     */
    Optional<Assignment> findByScheduleIdAndShiftIdAndEmployeeId(
            String scheduleId, String shiftId, String employeeId);

    void deleteById(String id);

    /**
     * Remove assignment by schedule + shift + employee composite.
     * Used by DELETE /schedules/{id}/assign.
     */
    void deleteByScheduleIdAndShiftIdAndEmployeeId(
            String scheduleId, String shiftId, String employeeId);

    boolean existsById(String id);
}
