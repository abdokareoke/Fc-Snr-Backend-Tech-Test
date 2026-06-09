package com.flightcentre.scheduler.exception;

import com.flightcentre.scheduler.model.valueobjects.Violation;
import java.util.List;

/**
 * Thrown when a business rule is violated during assignment.
 * Maps to HTTP 400 in the controller layer.
 * Carries the list of violations for the response body.
 */
public class RuleViolationException extends RuntimeException {

    private final List<Violation> violations;

    public RuleViolationException(String message, List<Violation> violations) {
        super(message);
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
