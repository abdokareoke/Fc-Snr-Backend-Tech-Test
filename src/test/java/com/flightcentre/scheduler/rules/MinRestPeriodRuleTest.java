package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.rules.impl.MinRestPeriodRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RULE-02: Minimum Rest Period (11 hours between shifts)
 * Scenario B from the spec: emp-02 assigned to shf-02 (Mon 14:00-22:00)
 * and shf-04 (Tue 06:00-14:00) — gap is 8 hours, should produce REST_VIOLATION.
 */
class MinRestPeriodRuleTest {

    private MinRestPeriodRule rule;
    private Employee employee;

    @BeforeEach
    void setUp() {
        rule = new MinRestPeriodRule();
        employee = new Employee("emp-02", "Bob Smith", "bob@example.com",
                ContractType.FULL_TIME, 40.0, List.of(), List.of());
    }

    @Test
    void shouldPassWhenNoExistingAssignments() {
        Shift candidate = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00");
        RuleContext context = buildContext(candidate, List.of(), Map.of(candidate.getId(), candidate));

        assertTrue(rule.evaluate(context).isEmpty());
    }

    @Test
    void shouldPassWhenRestPeriodIsSufficient() {
        // shf-02 ends Mon 22:00, candidate starts Tue 10:00 = 12 hours rest
        Shift candidate = shift("shf-04", "2026-03-31T10:00", "2026-03-31T18:00");
        Shift existing = shift("shf-02", "2026-03-30T14:00", "2026-03-30T22:00");
        Assignment existingAssignment = new Assignment("a1", "sch-01", existing.getId(), employee.getId());

        RuleContext context = buildContext(candidate,
                List.of(existingAssignment),
                Map.of(candidate.getId(), candidate, existing.getId(), existing));

        assertTrue(rule.evaluate(context).isEmpty(), "12 hours rest should pass");
    }

    @Test
    void shouldPassWhenExactly11HoursRest() {
        // shf-prev ends Mon 22:00, candidate starts Tue 09:00 = exactly 11 hours — boundary, should pass
        Shift candidate = shift("shf-next", "2026-03-31T09:00", "2026-03-31T17:00");
        Shift existing = shift("shf-prev", "2026-03-30T14:00", "2026-03-30T22:00");
        Assignment existingAssignment = new Assignment("a1", "sch-01", existing.getId(), employee.getId());

        RuleContext context = buildContext(candidate,
                List.of(existingAssignment),
                Map.of(candidate.getId(), candidate, existing.getId(), existing));

        assertTrue(rule.evaluate(context).isEmpty(), "Exactly 11 hours rest should pass");
    }

    @Test
    void shouldViolateWhenRestPeriodIsInsufficient() {
        // Scenario B: shf-02 ends Mon 22:00, shf-04 starts Tue 06:00 = 8 hours rest
        Shift candidate = shift("shf-04", "2026-03-31T06:00", "2026-03-31T14:00");
        Shift existing = shift("shf-02", "2026-03-30T14:00", "2026-03-30T22:00");
        Assignment existingAssignment = new Assignment("a1", "sch-01", existing.getId(), employee.getId());

        RuleContext context = buildContext(candidate,
                List.of(existingAssignment),
                Map.of(candidate.getId(), candidate, existing.getId(), existing));

        List<Violation> violations = rule.evaluate(context);

        assertEquals(1, violations.size());
        assertEquals("REST_VIOLATION", violations.get(0).getRule());
        assertEquals(Violation.Severity.ERROR, violations.get(0).getSeverity());
        assertEquals("emp-02", violations.get(0).getAffectedEmployeeId());
        assertTrue(violations.get(0).getAffectedShiftIds().contains("shf-02"));
        assertTrue(violations.get(0).getAffectedShiftIds().contains("shf-04"));
    }

    @Test
    void shouldViolateWhenCandidateEndsAndExistingStartsTooSoon() {
        // Reverse order: candidate ends first, existing starts too soon after
        // candidate ends Mon 14:00, existing starts Mon 22:00 = 8 hours rest
        Shift candidate = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00");
        Shift existing = shift("shf-02", "2026-03-30T22:00", "2026-03-30T22:00");
        Shift existingReal = shift("shf-02", "2026-03-30T20:00", "2026-03-31T04:00");
        Assignment existingAssignment = new Assignment("a1", "sch-01", existingReal.getId(), employee.getId());

        RuleContext context = buildContext(candidate,
                List.of(existingAssignment),
                Map.of(candidate.getId(), candidate, existingReal.getId(), existingReal));

        List<Violation> violations = rule.evaluate(context);

        assertEquals(1, violations.size());
        assertEquals("REST_VIOLATION", violations.get(0).getRule());
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
