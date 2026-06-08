package com.flightcentre.scheduler.service;

import com.flightcentre.scheduler.exception.ResourceNotFoundException;
import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.repository.AssignmentRepository;
import com.flightcentre.scheduler.repository.EmployeeRepository;
import com.flightcentre.scheduler.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for Employee operations.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;
    private final ShiftRepository shiftRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           AssignmentRepository assignmentRepository,
                           ShiftRepository shiftRepository) {
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.shiftRepository = shiftRepository;
    }

    public Employee create(Employee employee) {
        if (employee.getId() == null || employee.getId().isBlank()) {
            employee.setId("emp-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Employee with email " + employee.getEmail() + " already exists");
        }

        return employeeRepository.save(employee);
    }

    public Employee getById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    /**
     * GET /employees/{id}/schedule?week=2026-W14
     * Returns the shifts assigned to this employee for a given ISO week.
     */
    public List<Shift> getScheduleForEmployee(String employeeId, String weekParam) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }

        List<Assignment> assignments = assignmentRepository.findByEmployeeId(employeeId);

        return assignments.stream()
                .map(a -> shiftRepository.findById(a.getShiftId()).orElse(null))
                .filter(shift -> shift != null)
                .filter(shift -> weekParam == null || matchesWeek(shift, weekParam))
                .toList();
    }

    /**
     * Checks if a shift falls within the given ISO week string (e.g. "2026-W14").
     */
    private boolean matchesWeek(Shift shift, String weekParam) {
        try {
            // Parse "2026-W14" format
            String[] parts = weekParam.split("-W");
            int year = Integer.parseInt(parts[0]);
            int week = Integer.parseInt(parts[1]);

            java.time.LocalDate shiftDate = shift.getStartDatetime().toLocalDate();
            java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;

            return shiftDate.getYear() == year
                    && shiftDate.get(weekFields.weekOfWeekBasedYear()) == week;
        } catch (Exception e) {
            // If week param is malformed, return all shifts
            return true;
        }
    }
}
