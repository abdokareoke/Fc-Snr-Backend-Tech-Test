package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.rules.impl.WeeklyHoursLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RULE-03: Weekly Hours Limit (rolling 7-day window)
 */
class WeeklyHoursLimitRuleTest {

    private WeeklyHoursLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new WeeklyHoursLimitRule();
    }

    @Test
    void shouldPassWhenNoExistingAssignments() {
        Employee employee = employeeWithMaxHours(40.0);
        Shift candidate = shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00"); // 8h

        RuleContext context = new RuleContext(
                new Assignment("new", "sch-01", candidate.getId(), employee.getId()),
                employee, candidate, List.of(), Map.of(candidate.getId(), candidate), Map.of());

        assertTrue(rule.evaluate(context).isEmpty(), "Single shift under limit should pass");
    }

    @Test
    void shouldPassWhenUnderWeeklyLimit() {
        Employee employee = employeeWithMaxHours(40.0);
        // 4 x 8-hour shifts = 32 hours, under 40h limit
        Shift candidate = shift("shf-05", "2026-04-03T06:00", "2026-04-03T14:00"); // 8h
        List<Assignment> existing = List.of(
                assignmentFor("shf-01"), assignmentFor("shf-02"), assignmentFor("shf-03")
        );
        Map<String, Shift> allShifts = Map.of(
                "shf-01", shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00"),
                "shf-02", shift("shf-02", "2026-03-31T06:00", "2026-03-31T14:00"),
                "shf-03", shift("shf-03", "2026-04-01T06:00", "2026-04-01T14:00"),
                "shf-05", candidate
        );

        RuleContext context = new RuleContext(
                new Assignment("new", "sch-01", candidate.getId(), employee.getId()),
                employee, candidate, existing, allShifts, Map.of());

        assertTrue(rule.evaluate(context).isEmpty(), "32 hours should be under 40h limit");
    }

    @Test
    void shouldPassWhenExactlyAtWeeklyLimit() {
        // 5 x 8-hour shifts = exactly 40h — boundary, should pass
        Employee employee = employeeWithMaxHours(40.0);
        Shift candidate = shift("shf-05", "2026-04-03T06:00", "2026-04-03T14:00"); // 8h
        List<Assignment> existing = List.of(
                assignmentFor("shf-01"), assignmentFor("shf-02"),
                assignmentFor("shf-03"), assignmentFor("shf-04")
        );
        Map<String, Shift> allShifts = Map.of(
                "shf-01", shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00"),
                "shf-02", shift("shf-02", "2026-03-31T06:00", "2026-03-31T14:00"),
                "shf-03", shift("shf-03", "2026-04-01T06:00", "2026-04-01T14:00"),
                "shf-04", shift("shf-04", "2026-04-02T06:00", "2026-04-02T14:00"),
                "shf-05", candidate
        );

        RuleContext context = new RuleContext(
                new Assignment("new", "sch-01", candidate.getId(), employee.getId()),
                employee, candidate, existing, allShifts, Map.of());

        assertTrue(rule.evaluate(context).isEmpty(), "Exactly 40h should pass (not strictly greater than)");
    }

    @Test
    void shouldViolateWhenOverWeeklyLimit() {
        Employee employee = employeeWithMaxHours(20.0); // Part-time, 20h max
        // 3 x 8-hour shifts already = 24h, adding one more would be 32h — over 20h
        Shift candidate = shift("shf-04", "2026-04-02T06:00", "2026-04-02T14:00"); // 8h
        List<Assignment> existing = List.of(
                assignmentFor("shf-01"), assignmentFor("shf-02"), assignmentFor("shf-03")
        );
        Map<String, Shift> allShifts = Map.of(
                "shf-01", shift("shf-01", "2026-03-30T06:00", "2026-03-30T14:00"),
                "shf-02", shift("shf-02", "2026-03-31T06:00", "2026-03-31T14:00"),
                "shf-03", shift("shf-03", "2026-04-01T06:00", "2026-04-01T14:00"),
                "shf-04", candidate
        );

        RuleContext context = new RuleContext(
                new Assignment("new", "sch-01", candidate.getId(), employee.getId()),
                employee, candidate, existing, allShifts, Map.of());

        List<Violation> violations = rule.evaluate(context);

        assertEquals(1, violations.size());
        assertEquals("WEEKLY_HOURS_EXCEEDED", violations.get(0).getRule());
        assertEquals(Violation.Severity.ERROR, violations.get(0).getSeverity());
        assertEquals(employee.getId(), violations.get(0).getAffectedEmployeeId());
    }

    @Test
    void shouldNotCountShiftsOutsideRollingWindow() {
        Employee employee = employeeWithMaxHours(40.0);
        // Shift 10 days ago — outside the 168-hour window, should not count
        Shift candidate = shift("shf-new", "2026-04-07T06:00", "2026-04-07T14:00");
        Shift oldShift = shift("shf-old", "2026-03-26T06:00", "2026-03-26T14:00"); // 12 days ago
        List<Assignment> existing = List.of(assignmentFor("shf-old"));
        Map<String, Shift> allShifts = Map.of(
                "shf-old", oldShift,
                "shf-new", candidate
        );

        RuleContext context = new RuleContext(
                new Assignment("new", "sch-01", candidate.getId(), employee.getId()),
                employee, candidate, existing, allShifts, Map.of());

        assertTrue(rule.evaluate(context).isEmpty(), "Shift outside rolling window should not count");
    }

    // --- Helpers ---

    private Employee employeeWithMaxHours(double maxHours) {
        return new Employee("emp-03", "Carol White", "carol@example.com",
                ContractType.PART_TIME, maxHours, List.of(), List.of());
    }

    private Shift shift(String id, String start, String end) {
        return new Shift(id, "Warehouse A",
                LocalDateTime.parse(start), LocalDateTime.parse(end), 1, List.of());
    }

    private Assignment assignmentFor(String shiftId) {
        return new Assignment("a-" + shiftId, "sch-01", shiftId, "emp-03");
    }
}
