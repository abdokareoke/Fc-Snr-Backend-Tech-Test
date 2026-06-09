package com.flightcentre.scheduler.rules.impl;

import com.flightcentre.scheduler.model.valueobjects.Violation;
import com.flightcentre.scheduler.model.enums.Skill;
import com.flightcentre.scheduler.rules.BusinessRule;
import com.flightcentre.scheduler.rules.RuleContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RULE-04: Skill Match
 * An employee assigned to a shift must possess all skills required by that shift.
 * Assignments to shifts with no required skills are always valid for any employee.
 */
@Component
public class SkillMatchRule implements BusinessRule {

    @Override
    public String getRuleName() {
        return "SKILL_MISMATCH";
    }

    @Override
    public List<Violation> evaluate(RuleContext context) {
        List<Skill> requiredSkills = context.candidateShift().getRequiredSkills();

        // No required skills — any employee qualifies
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return List.of();
        }

        List<Skill> employeeSkills = context.employee().getSkills() != null
                ? context.employee().getSkills()
                : List.of();

        List<Skill> missingSkills = requiredSkills.stream()
                .filter(skill -> !employeeSkills.contains(skill))
                .toList();

        if (!missingSkills.isEmpty()) {
            return List.of(new Violation(
                    getRuleName(),
                    Violation.Severity.ERROR,
                    String.format("%s does not have the required skills for shift %s (missing: %s)",
                            context.employee().getName(),
                            context.candidateShift().getId(),
                            missingSkills),
                    context.employee().getId(),
                    List.of(context.candidateShift().getId())
            ));
        }

        return List.of();
    }
}
