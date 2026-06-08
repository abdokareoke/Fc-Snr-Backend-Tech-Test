package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.rules.impl.NoOverlapRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RULE-01: No Overlapping Shifts
 * Scenario A from the spec: emp-01 assigned to shf-01 (Mon 06:00-14:00)
 * and shf-03 (Mon 08:00-16:00) — should produce OVERLAP violation.
 */
class NoOverlapRuleTest {

    private NoOverlapRule rule;
    private Employee employee;

    @BeforeEach
    void setUp() {
        rule = new NoOverlapRule();
        employee = new Employee("emp-01", "Alice Johnson", "alice@example.com",
                ContractType.FULL_TIME, 40.0, List.of(), List.of());
    }

    @Test
    void shouldPassWhenNoExistingAssignments() {
        Shift shift = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00");
        RuleContext context = buildContext(shift, List.of(), Map.of(shift.getId(), shift));

        assertTrue(rule.evaluate(context).isEmpty(), "No violation expected with no existing assignments");
    }

    @Test
    void shouldPassWhenShiftsDoNotOverlap() {
        // shf-02 starts exactly when shf-01 ends — adjacent, not overlapping
        Shift candidate = shift("shf-02", "2026-03-30T14:00", "2026-03-30T22:00");
        Shift existing = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00");
        Assignment existingAssignment = new Assignment("a1", "sch-01", existing.getId(), employee.getId());

        RuleContext context = buildContext(candidate,
                List.of(existingAssignment),
                Map.of(candidate.getId(), candidate, existing.getId(), existing));

        assertTrue(rule.evaluate(context).isEmpty(), "Adjacent non-overlapping shifts should pass");
    }

    @Test
    void shouldViolateWhenShiftsOverlap() {
        // Scenario A: shf-01 (06:00-14:00) and shf-03 (08:00-16:00) overlap
        Shift candidate = shift("shf-03", "2026-03-30T08:00", "2026-03-30T16:00");
        Shift existing = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00");
        Assignment existingAssignment = new Assignment("a1", "sch-01", existing.getId(), employee.getId());

        RuleContext context = buildContext(candidate,
                List.of(existingAssignment),
                Map.of(candidate.getId(), candidate, existing.getId(), existing));

        List<Violation> violations = rule.evaluate(context);

        assertEquals(1, violations.size());
        assertEquals("OVERLAP", violations.get(0).getRule());
        assertEquals(Violation.Severity.ERROR, violations.get(0).getSeverity());
        assertEquals("emp-01", violations.get(0).getAffectedEmployeeId());
        assertTrue(violations.get(0).getAffectedShiftIds().contains("shf-01"));
        assertTrue(violations.get(0).getAffectedShiftIds().contains("shf-03"));
    }

    @Test
    void shouldReturnMultipleViolationsWhenOverlappingWithSeveralShifts() {
        // Candidate overlaps with two existing shifts simultaneously
        Shift candidate = shift("shf-new", "2026-03-30T08:00", "2026-03-30T18:00");
        Shift existing1 = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00"); // overlaps
        Shift existing2 = shift("shf-02", "2026-03-30T12:00", "2026-03-30T20:00"); // overlaps
        Shift existing3 = shift("shf-03", "2026-03-30T20:00", "2026-03-30T22:00"); // does not overlap

        List<Assignment> existingAssignments = List.of(
                new Assignment("a1", "sch-01", existing1.getId(), employee.getId()),
                new Assignment("a2", "sch-01", existing2.getId(), employee.getId()),
                new Assignment("a3", "sch-01", existing3.getId(), employee.getId())
        );

        RuleContext context = buildContext(candidate, existingAssignments,
                Map.of(candidate.getId(), candidate,
                        existing1.getId(), existing1,
                        existing2.getId(), existing2,
                        existing3.getId(), existing3));

        List<Violation> violations = rule.evaluate(context);

        assertEquals(2, violations.size(), "Should return one violation per overlapping shift");
    }

    @Test
    void shouldPassWhenEvaluatingSameShift() {
        // Re-evaluating an existing assignment should not self-conflict
        Shift shift = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00");
        Assignment existingAssignment = new Assignment("a1", "sch-01", shift.getId(), employee.getId());

        RuleContext context = buildContext(shift,
                List.of(existingAssignment),
                Map.of(shift.getId(), shift));

        assertTrue(rule.evaluate(context).isEmpty(), "A shift should not conflict with itself");
    }

    // --- Helpers ---

    private Shift shift(String id, String start, String end) {
        return new Shift(id, "Warehouse A",
                LocalDateTime.parse(start), LocalDateTime.parse(end), 1, List.of());
    }

    private RuleContext buildContext(Shift candidate, List<Assignment> existing, Map<String, Shift> allShifts) {
        Assignment candidateAssignment = new Assignment("new", "sch-01", candidate.getId(), employee.getId());
        return new RuleContext(candidateAssignment, employee, candidate, existing, allShifts, Map.of());
    }
}
