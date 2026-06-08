package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.model.enums.Skill;
import com.flightcentre.scheduler.rules.impl.SkillMatchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RULE-04: Skill Match
 * Scenario C from the spec: emp-03 (FIRST_AID only) assigned to shf-01
 * (requires FORKLIFT_CERTIFIED) — should produce SKILL_MISMATCH.
 */
class SkillMatchRuleTest {

    private SkillMatchRule rule;

    @BeforeEach
    void setUp() {
        rule = new SkillMatchRule();
    }

    @Test
    void shouldPassWhenEmployeeHasAllRequiredSkills() {
        Employee employee = employee(List.of(Skill.FORKLIFT_CERTIFIED, Skill.FIRST_AID));
        Shift shift = shiftRequiring(List.of(Skill.FORKLIFT_CERTIFIED));

        assertTrue(rule.evaluate(buildContext(employee, shift)).isEmpty());
    }

    @Test
    void shouldPassWhenShiftHasNoRequiredSkills() {
        // Any employee qualifies for a shift with no skill requirements
        Employee employee = employee(List.of(Skill.FIRST_AID));
        Shift shift = shiftRequiring(List.of());

        assertTrue(rule.evaluate(buildContext(employee, shift)).isEmpty());
    }

    @Test
    void shouldPassWhenEmployeeHasMoreSkillsThanRequired() {
        Employee employee = employee(List.of(Skill.FORKLIFT_CERTIFIED, Skill.FIRST_AID, Skill.SUPERVISOR));
        Shift shift = shiftRequiring(List.of(Skill.FORKLIFT_CERTIFIED));

        assertTrue(rule.evaluate(buildContext(employee, shift)).isEmpty());
    }

    @Test
    void shouldViolateWhenEmployeeMissingRequiredSkill() {
        // Scenario C: emp-03 has FIRST_AID, shift requires FORKLIFT_CERTIFIED
        Employee employee = employee(List.of(Skill.FIRST_AID));
        Shift shift = shiftRequiring(List.of(Skill.FORKLIFT_CERTIFIED));

        List<Violation> violations = rule.evaluate(buildContext(employee, shift));

        assertEquals(1, violations.size());
        assertEquals("SKILL_MISMATCH", violations.get(0).getRule());
        assertEquals(Violation.Severity.ERROR, violations.get(0).getSeverity());
        assertEquals(employee.getId(), violations.get(0).getAffectedEmployeeId());
        assertTrue(violations.get(0).getAffectedShiftIds().contains(shift.getId()));
    }

    @Test
    void shouldViolateWhenEmployeeHasNoSkillsAtAll() {
        Employee employee = employee(List.of());
        Shift shift = shiftRequiring(List.of(Skill.SUPERVISOR));

        List<Violation> violations = rule.evaluate(buildContext(employee, shift));

        assertEquals(1, violations.size());
        assertEquals("SKILL_MISMATCH", violations.get(0).getRule());
    }

    // --- Helpers ---

    private Employee employee(List<Skill> skills) {
        return new Employee("emp-03", "Carol White", "carol@example.com",
                ContractType.PART_TIME, 20.0, skills, List.of());
    }

    private Shift shiftRequiring(List<Skill> skills) {
        return new Shift("shf-01", "Warehouse A",
                LocalDateTime.parse("2026-03-30T06:00"),
                LocalDateTime.parse("2026-03-30T14:00"),
                2, skills);
    }

    private RuleContext buildContext(Employee employee, Shift shift) {
        return new RuleContext(
                new Assignment("new", "sch-01", shift.getId(), employee.getId()),
                employee, shift, List.of(), Map.of(shift.getId(), shift), Map.of());
    }
}
