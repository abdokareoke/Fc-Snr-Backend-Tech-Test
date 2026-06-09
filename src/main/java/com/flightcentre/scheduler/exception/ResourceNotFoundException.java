package com.flightcentre.scheduler.exception;

/**
 * Thrown when a requested resource (employee, shift, schedule) does not exist.
 * Maps to HTTP 404 in the controller layer.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
