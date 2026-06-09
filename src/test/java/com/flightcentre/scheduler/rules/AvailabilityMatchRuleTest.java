package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.model.valueobjects.AvailabilityWindow;
import com.flightcentre.scheduler.rules.impl.AvailabilityMatchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RULE-06: Availability Matching
 * Scenario E from the spec: emp-01 (Mon-Fri 06:00-22:00) assigned to a
 * Saturday shift — should produce AVAILABILITY_VIOLATION.
 */
class AvailabilityMatchRuleTest {

    private AvailabilityMatchRule rule;
    private List<AvailabilityWindow> monToFriAvailability;

    @BeforeEach
    void setUp() {
        rule = new AvailabilityMatchRule();
        // Standard Mon-Fri 06:00-22:00 availability (matches seed data)
        monToFriAvailability = List.of(
                new AvailabilityWindow(DayOfWeek.MONDAY,    LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.TUESDAY,   LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.WEDNESDAY, LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.THURSDAY,  LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.FRIDAY,    LocalTime.of(6, 0), LocalTime.of(22, 0))
        );
    }

    @Test
    void shouldPassWhenShiftFallsWithinAvailability() {
        // Mon 08:00-16:00 within Mon 06:00-22:00
        Employee employee = employeeWithAvailability(monToFriAvailability);
        Shift shift = shift("shf-03", "2026-03-30T08:00", "2026-03-30T16:00"); // Monday

        assertTrue(rule.evaluate(buildContext(employee, shift)).isEmpty());
    }

    @Test
    void shouldPassWhenShiftExactlyMatchesAvailabilityBoundary() {
        // Mon 06:00-22:00 exactly matches window boundaries
        Employee employee = employeeWithAvailability(monToFriAvailability);
        Shift shift = shift("shf-exact", "2026-03-30T06:00", "2026-03-30T22:00"); // Monday

        assertTrue(rule.evaluate(buildContext(employee, shift)).isEmpty());
    }

    @Test
    void shouldViolateWhenShiftOnUnavailableDay() {
        // Scenario E: Saturday shift, employee only available Mon-Fri
        Employee employee = employeeWithAvailability(monToFriAvailability);
        Shift shift = shift("shf-sat", "2026-04-04T08:00", "2026-04-04T16:00"); // Saturday

        List<Violation> violations = rule.evaluate(buildContext(employee, shift));

        assertEquals(1, violations.size());
        assertEquals("AVAILABILITY_VIOLATION", violations.get(0).getRule());
        assertEquals(Violation.Severity.ERROR, violations.get(0).getSeverity());
        assertEquals(employee.getId(), violations.get(0).getAffectedEmployeeId());
        assertTrue(violations.get(0).getAffectedShiftIds().contains("shf-sat"));
    }

    @Test
    void shouldViolateWhenShiftStartsBeforeAvailability() {
        // Shift starts 05:00, availability starts 06:00
        Employee employee = employeeWithAvailability(monToFriAvailability);
        Shift shift = shift("shf-early", "2026-03-30T05:00", "2026-03-30T13:00"); // Monday

        List<Violation> violations = rule.evaluate(buildContext(employee, shift));

        assertEquals(1, violations.size());
        assertEquals("AVAILABILITY_VIOLATION", violations.get(0).getRule());
    }

    @Test
    void shouldViolateWhenShiftEndsAfterAvailability() {
        // Shift ends 23:00, availability ends 22:00
        Employee employee = employeeWithAvailability(monToFriAvailability);
        Shift shift = shift("shf-late", "2026-03-30T14:00", "2026-03-30T23:00"); // Monday

        List<Violation> violations = rule.evaluate(buildContext(employee, shift));

        assertEquals(1, violations.size());
        assertEquals("AVAILABILITY_VIOLATION", violations.get(0).getRule());
    }

    @Test
    void shouldViolateWhenEmployeeHasNoAvailability() {
        Employee employee = employeeWithAvailability(List.of());
        Shift shift = shift("shf-01", "2026-03-30T08:00", "2026-03-30T16:00");

        List<Violation> violations = rule.evaluate(buildContext(employee, shift));

        assertEquals(1, violations.size());
        assertEquals("AVAILABILITY_VIOLATION", violations.get(0).getRule());
    }

    @Test
    void shouldPassWhenOvernightShiftCoversAvailabilityOnBothDays() {
        // shf-05 from seed data: Tue 22:00 - Wed 06:00
        // Employee available Tue 06:00-22:00 would FAIL (shift starts at end of window)
        // Employee available Tue 06:00-23:59 and Wed 00:00-06:00 would PASS
        List<AvailabilityWindow> overnightAvailability = List.of(
                new AvailabilityWindow(DayOfWeek.TUESDAY,   LocalTime.of(22, 0), LocalTime.MAX),
                new AvailabilityWindow(DayOfWeek.WEDNESDAY, LocalTime.MIDNIGHT,  LocalTime.of(6, 0))
        );
        Employee employee = employeeWithAvailability(overnightAvailability);
        Shift shift = shift("shf-05", "2026-03-31T22:00", "2026-04-01T06:00"); // Tue 22:00 - Wed 06:00

        assertTrue(rule.evaluate(buildContext(employee, shift)).isEmpty(),
                "Overnight shift within availability on both days should pass");
    }

    @Test
    void shouldViolateWhenOvernightShiftExceedsAvailabilityOnEndDay() {
        // Overnight shift: Tue 22:00 - Wed 06:00
        // Employee available Tue but NOT Wed — violation
        List<AvailabilityWindow> tuesdayOnly = List.of(
                new AvailabilityWindow(DayOfWeek.TUESDAY, LocalTime.of(6, 0), LocalTime.MAX)
        );
        Employee employee = employeeWithAvailability(tuesdayOnly);
        Shift shift = shift("shf-05", "2026-03-31T22:00", "2026-04-01T06:00"); // Tue 22:00 - Wed 06:00

        List<Violation> violations = rule.evaluate(buildContext(employee, shift));

        assertEquals(1, violations.size());
        assertEquals("AVAILABILITY_VIOLATION", violations.get(0).getRule());
    }

    // --- Helpers ---

    private Employee employeeWithAvailability(List<AvailabilityWindow> windows) {
        return new Employee("emp-01", "Alice Johnson", "alice@example.com",
                ContractType.FULL_TIME, 40.0, List.of(), windows);
    }

    private Shift shift(String id, String start, String end) {
        return new Shift(id, "Office",
                LocalDateTime.parse(start), LocalDateTime.parse(end), 1, List.of());
    }

    private RuleContext buildContext(Employee employee, Shift shift) {
        return new RuleContext(
                new Assignment("new", "sch-01", shift.getId(), employee.getId()),
                employee, shift, List.of(), Map.of(shift.getId(), shift), Map.of());
    }
}
