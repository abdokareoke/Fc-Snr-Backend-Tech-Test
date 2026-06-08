package com.flightcentre.scheduler.service;

import com.flightcentre.scheduler.exception.ResourceNotFoundException;
import com.flightcentre.scheduler.exception.RuleViolationException;
import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Schedule;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.ConflictReport;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.repository.AssignmentRepository;
import com.flightcentre.scheduler.repository.EmployeeRepository;
import com.flightcentre.scheduler.repository.ScheduleRepository;
import com.flightcentre.scheduler.repository.ShiftRepository;
import com.flightcentre.scheduler.rules.RuleContext;
import com.flightcentre.scheduler.rules.RuleEvaluator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for Schedule operations including:
 * - Assignment validation (POST /schedules/{id}/assign)
 * - Conflict detection (GET /schedules/{id}/conflicts)
 * - Auto-assign (POST /schedules/{id}/auto-assign)
 */
@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;
    private final RuleEvaluator ruleEvaluator;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           ShiftRepository shiftRepository,
                           EmployeeRepository employeeRepository,
                           AssignmentRepository assignmentRepository,
                           RuleEvaluator ruleEvaluator) {
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.ruleEvaluator = ruleEvaluator;
    }

    public Schedule create(Schedule schedule) {
        if (schedule.getId() == null || schedule.getId().isBlank()) {
            schedule.setId("sch-" + UUID.randomUUID().toString().substring(0, 8));
        }

        return scheduleRepository.save(schedule);
    }

    public Schedule getById(String id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
    }

    /**
     * Manually assign an employee to a shift within this schedule.
     * Validates against all business rules before saving.
     * Returns the created assignment or throws if ERROR-level violations exist.
     */
    public Assignment assignEmployeeToShift(String scheduleId, String employeeId, String shiftId) {
        // Validate entities exist
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + shiftId));

        // Check the shift belongs to this schedule
        if (!schedule.getShiftIds().contains(shiftId)) {
            throw new IllegalArgumentException("Shift " + shiftId + " does not belong to schedule " + scheduleId);
        }

        // Check for duplicate assignment
        if (assignmentRepository.findByScheduleIdAndShiftIdAndEmployeeId(scheduleId, shiftId, employeeId).isPresent()) {
            throw new IllegalArgumentException("Employee " + employeeId + " is already assigned to shift " + shiftId);
        }

        // Build rule context and evaluate
        Assignment candidateAssignment = new Assignment(
                "asgn-" + UUID.randomUUID().toString().substring(0, 8),
                scheduleId, shiftId, employeeId);

        RuleContext context = buildRuleContext(candidateAssignment, employee, shift);
        List<Violation> violations = ruleEvaluator.evaluate(context);

        // Block on ERROR violations only; WARNINGs (understaffed) are allowed
        List<Violation> errors = violations.stream()
                .filter(v -> v.getSeverity() == Violation.Severity.ERROR)
                .toList();

        if (!errors.isEmpty()) {
            throw new RuleViolationException("Assignment violates business rules", errors);
        }

        return assignmentRepository.save(candidateAssignment);
    }

    /**
     * Remove an assignment from the schedule.
     */
    public void removeAssignment(String scheduleId, String employeeId, String shiftId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found: " + scheduleId);
        }

        assignmentRepository.deleteByScheduleIdAndShiftIdAndEmployeeId(scheduleId, shiftId, employeeId);
    }

    /**
     * Detect all rule violations in the current schedule.
     * Evaluates each existing assignment against all 6 rules.
     */
    public ConflictReport detectConflicts(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

        List<Assignment> scheduleAssignments = assignmentRepository.findByScheduleId(scheduleId);
        List<Shift> scheduleShifts = shiftRepository.findAllById(schedule.getShiftIds());
        Map<String, Shift> shiftsMap = scheduleShifts.stream()
                .collect(Collectors.toMap(Shift::getId, s -> s));
        Map<String, List<Assignment>> assignmentsByShift = scheduleAssignments.stream()
                .collect(Collectors.groupingBy(Assignment::getShiftId));

        List<Violation> allViolations = new ArrayList<>();

        // Evaluate each existing assignment as if it were a candidate
        for (Assignment assignment : scheduleAssignments) {
            Employee employee = employeeRepository.findById(assignment.getEmployeeId()).orElse(null);
            Shift shift = shiftsMap.get(assignment.getShiftId());
            if (employee == null || shift == null) continue;

            // Existing assignments for this employee (excluding the one being evaluated)
            List<Assignment> otherAssignments = assignmentRepository.findByEmployeeId(assignment.getEmployeeId())
                    .stream()
                    .filter(a -> !a.getId().equals(assignment.getId()))
                    .toList();

            // assignmentsByShift should exclude the current assignment for headcount check
            Map<String, List<Assignment>> adjustedAssignmentsByShift = assignmentsByShift.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .filter(a -> !a.getId().equals(assignment.getId()))
                                    .toList()
                    ));

            RuleContext context = new RuleContext(
                    assignment, employee, shift, otherAssignments, shiftsMap, adjustedAssignmentsByShift);

            allViolations.addAll(ruleEvaluator.evaluate(context));
        }

        // Check headcount for shifts with NO assignments (completely unstaffed)
        for (Shift shift : scheduleShifts) {
            List<Assignment> shiftAssignments = assignmentsByShift.getOrDefault(shift.getId(), List.of());
            if (shiftAssignments.isEmpty() && shift.getRequiredHeadcount() > 0) {
                allViolations.add(new Violation(
                        "UNDERSTAFFED",
                        Violation.Severity.WARNING,
                        String.format("Shift %s requires %d employees but only 0 are assigned",
                                shift.getId(), shift.getRequiredHeadcount()),
                        null,
                        List.of(shift.getId())
                ));
            }
        }

        // Deduplicate violations (same rule + same shifts + same employee = same violation)
        List<Violation> deduped = allViolations.stream()
                .distinct()
                .toList();

        return new ConflictReport(scheduleId, LocalDateTime.now(), deduped);
    }

    /**
     * Auto-assign employees to shifts, prioritizing understaffed shifts.
     * Respects all 6 business rules.
     */
    public Schedule autoAssign(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

        List<Shift> shifts = shiftRepository.findAllById(schedule.getShiftIds());
        List<Employee> employees = employeeRepository.findAll();

        // Sort shifts by how understaffed they are (most understaffed first)
        shifts.sort(Comparator.comparingInt((Shift s) -> {
            int assigned = assignmentRepository.findByShiftId(s.getId()).size();
            return assigned - s.getRequiredHeadcount(); // more negative = more understaffed
        }));

        for (Shift shift : shifts) {
            List<Assignment> currentAssignments = assignmentRepository.findByShiftId(shift.getId());
            int needed = shift.getRequiredHeadcount() - currentAssignments.size();

            if (needed <= 0) continue; // shift is fully staffed

            for (Employee employee : employees) {
                if (needed <= 0) break;

                // Skip if already assigned to this shift
                boolean alreadyAssigned = currentAssignments.stream()
                        .anyMatch(a -> a.getEmployeeId().equals(employee.getId()));
                if (alreadyAssigned) continue;

                // Build context and check rules
                Assignment candidateAssignment = new Assignment(
                        "asgn-" + UUID.randomUUID().toString().substring(0, 8),
                        scheduleId, shift.getId(), employee.getId());

                RuleContext context = buildRuleContext(candidateAssignment, employee, shift);
                List<Violation> violations = ruleEvaluator.evaluate(context);

                boolean hasErrors = violations.stream()
                        .anyMatch(v -> v.getSeverity() == Violation.Severity.ERROR);

                if (!hasErrors) {
                    assignmentRepository.save(candidateAssignment);
                    currentAssignments = assignmentRepository.findByShiftId(shift.getId());
                    needed--;
                }
            }
        }

        return schedule;
    }

    /**
     * Build a RuleContext for evaluating a candidate assignment.
     */
    private RuleContext buildRuleContext(Assignment candidateAssignment, Employee employee, Shift shift) {
        List<Assignment> existingAssignments = assignmentRepository.findByEmployeeId(employee.getId());

        // Build a map of all shifts referenced by the employee's existing assignments + the candidate
        Map<String, Shift> allShifts = existingAssignments.stream()
                .map(a -> shiftRepository.findById(a.getShiftId()).orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toMap(Shift::getId, s -> s, (a, b) -> a));
        allShifts.put(shift.getId(), shift);

        // Assignments grouped by shift (for headcount checks)
        Map<String, List<Assignment>> assignmentsByShift = Map.of(
                shift.getId(), assignmentRepository.findByShiftId(shift.getId()));

        return new RuleContext(candidateAssignment, employee, shift, existingAssignments, allShifts, assignmentsByShift);
    }
}
