package com.company.booking.listener;

/**
 * Thrown when a floor is saved with a plan that is not a PNG or JPEG file.
 */
public class InvalidFloorPlanException extends RuntimeException {

    public InvalidFloorPlanException(final String message) {
        super(message);
    }
}
