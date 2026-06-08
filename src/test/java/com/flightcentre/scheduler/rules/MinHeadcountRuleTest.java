package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.rules.impl.MinHeadcountRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RULE-05: Minimum Headcount
 * Scenario D from the spec: only emp-02 assigned to shf-01 (requires headcount of 2)
 * — should produce UNDERSTAFFED warning.
 */
class MinHeadcountRuleTest {

    private MinHeadcountRule rule;
    private Employee employee;

    @BeforeEach
    void setUp() {
        rule = new MinHeadcountRule();
        employee = new Employee("emp-02", "Bob Smith", "bob@example.com",
                ContractType.FULL_TIME, 40.0, List.of(), List.of());
    }

    @Test
    void shouldPassWhenHeadcountMet() {
        // Shift requires 2, already has 1 assigned, candidate is the 2nd
        Shift shift = shiftWithHeadcount("shf-01", 2);
        Assignment existingAssignment = new Assignment("a1", "sch-01", shift.getId(), "emp-01");
        Map<String, List<Assignment>> assignmentsByShift = Map.of(shift.getId(), List.of(existingAssignment));

        RuleContext context = buildContext(shift, assignmentsByShift);

        assertTrue(rule.evaluate(context).isEmpty(), "Headcount met with 2 employees");
    }

    @Test
    void shouldPassWhenHeadcountExceeded() {
        // Shift requires 1, already has 2 — still valid (over-staffing is allowed)
        Shift shift = shiftWithHeadcount("shf-01", 1);
        Map<String, List<Assignment>> assignmentsByShift = Map.of(
                shift.getId(), List.of(
                        new Assignment("a1", "sch-01", shift.getId(), "emp-01"),
                        new Assignment("a2", "sch-01", shift.getId(), "emp-03")
                )
        );

        RuleContext context = buildContext(shift, assignmentsByShift);

        assertTrue(rule.evaluate(context).isEmpty(), "Over-staffing should not trigger a violation");
    }

    @Test
    void shouldWarnWhenUnderstaffed() {
        // Scenario D: shf-01 requires 2, only 1 being assigned (no previous assignments)
        Shift shift = shiftWithHeadcount("shf-01", 2);
        Map<String, List<Assignment>> assignmentsByShift = Map.of(shift.getId(), List.of());

        RuleContext context = buildContext(shift, assignmentsByShift);

        List<Violation> violations = rule.evaluate(context);

        assertEquals(1, violations.size());
        assertEquals("UNDERSTAFFED", violations.get(0).getRule());
        assertEquals(Violation.Severity.WARNING, violations.get(0).getSeverity(),
                "Understaffed should be a WARNING, not an ERROR");
        assertNull(violations.get(0).getAffectedEmployeeId(),
                "No specific employee for headcount violation");
        assertTrue(violations.get(0).getAffectedShiftIds().contains("shf-01"));
    }

    @Test
    void shouldPassWhenShiftRequiresOneAndCandidateIsFirst() {
        Shift shift = shiftWithHeadcount("shf-01", 1);
        Map<String, List<Assignment>> assignmentsByShift = Map.of(shift.getId(), List.of());

        RuleContext context = buildContext(shift, assignmentsByShift);

        assertTrue(rule.evaluate(context).isEmpty(), "Single required employee being assigned should pass");
    }

    // --- Helpers ---

    private Shift shiftWithHeadcount(String id, int headcount) {
        return new Shift(id, "Warehouse A",
                LocalDateTime.parse("2026-03-30T06:00"),
                LocalDateTime.parse("2026-03-30T14:00"),
                headcount, List.of());
    }

    private RuleContext buildContext(Shift shift, Map<String, List<Assignment>> assignmentsByShift) {
        return new RuleContext(
                new Assignment("new", "sch-01", shift.getId(), employee.getId()),
                employee, shift, List.of(), Map.of(shift.getId(), shift), assignmentsByShift);
    }
}
