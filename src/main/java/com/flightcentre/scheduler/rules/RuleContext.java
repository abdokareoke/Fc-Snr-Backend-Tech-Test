package com.flightcentre.scheduler.rules;

import com.flightcentre.scheduler.model.Assignment;
import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Shift;
import java.util.List;
import java.util.Map;

/**
 * Carries all the data a business rule needs to evaluate an assignment.
 *
 * Rather than passing many parameters to each rule, we bundle the context
 * into one object. This keeps the BusinessRule interface clean and makes
 * it easy to add new context data later without changing every rule's signature.
 *
 * @param candidateAssignment  The assignment being evaluated (may not be saved yet)
 * @param employee             The employee being assigned
 * @param candidateShift       The shift being assigned to
 * @param existingAssignments  All existing assignments for this employee (for overlap/rest/hours checks)
 * @param allShifts            Map of shiftId -> Shift for resolving shift details
 * @param assignmentsByShift   Map of shiftId -> List<Assignment> for headcount checks
 */
public record RuleContext(
        Assignment candidateAssignment,
        Employee employee,
        Shift candidateShift,
        List<Assignment> existingAssignments,
        Map<String, Shift> allShifts,
        Map<String, List<Assignment>> assignmentsByShift
) {}
