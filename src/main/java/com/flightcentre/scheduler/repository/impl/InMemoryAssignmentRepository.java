package com.flightcentre.scheduler.repository.impl;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.repository.AssignmentRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of AssignmentRepository.
 *
 * Maintains two secondary indexes for the most frequent query patterns
 * used by the business rule engine:
 *
 *   employeeIndex  : employeeId  -> List<Assignment>
 *                    Used by RULE-01 (overlap), RULE-02 (rest), RULE-03 (weekly hours)
 *
 *   scheduleIndex  : scheduleId  -> List<Assignment>
 *                    Used by conflict detection and GET /schedules/{id}
 *
 * These indexes prevent full table scans on every rule evaluation,
 * keeping rule checks at O(n) where n is assignments per employee/schedule
 * rather than O(total assignments).
 */
@Repository
public class InMemoryAssignmentRepository implements AssignmentRepository {

    private final Map<String, Assignment> store = new ConcurrentHashMap<>();
    private final Map<String, List<Assignment>> employeeIndex = new ConcurrentHashMap<>();
    private final Map<String, List<Assignment>> scheduleIndex = new ConcurrentHashMap<>();

    @Override
    public Assignment save(Assignment assignment) {
        store.put(assignment.getId(), assignment);
        employeeIndex
                .computeIfAbsent(assignment.getEmployeeId(), k -> new ArrayList<>())
                .add(assignment);
        scheduleIndex
                .computeIfAbsent(assignment.getScheduleId(), k -> new ArrayList<>())
                .add(assignment);
        return assignment;
    }

    @Override
    public Optional<Assignment> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Assignment> findByEmployeeId(String employeeId) {
        return employeeIndex.getOrDefault(employeeId, List.of());
    }

    @Override
    public List<Assignment> findByScheduleId(String scheduleId) {
        return scheduleIndex.getOrDefault(scheduleId, List.of());
    }

    @Override
    public List<Assignment> findByShiftId(String shiftId) {
        // No dedicated index for shift — shifts typically have few assignments
        // (bounded by requiredHeadcount), so a filtered scan is acceptable here.
        return store.values().stream()
                .filter(a -> a.getShiftId().equals(shiftId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Assignment> findByScheduleIdAndShiftIdAndEmployeeId(
            String scheduleId, String shiftId, String employeeId) {
        return scheduleIndex.getOrDefault(scheduleId, List.of()).stream()
                .filter(a -> a.getShiftId().equals(shiftId)
                        && a.getEmployeeId().equals(employeeId))
                .findFirst();
    }

    @Override
    public void deleteById(String id) {
        Assignment assignment = store.remove(id);
        if (assignment != null) {
            removeFromIndex(employeeIndex, assignment.getEmployeeId(), assignment);
            removeFromIndex(scheduleIndex, assignment.getScheduleId(), assignment);
        }
    }

    @Override
    public void deleteByScheduleIdAndShiftIdAndEmployeeId(
            String scheduleId, String shiftId, String employeeId) {
        findByScheduleIdAndShiftIdAndEmployeeId(scheduleId, shiftId, employeeId)
                .ifPresent(a -> deleteById(a.getId()));
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    private void removeFromIndex(Map<String, List<Assignment>> index,
                                  String key, Assignment assignment) {
        List<Assignment> list = index.get(key);
        if (list != null) {
            list.remove(assignment);
        }
    }
}
