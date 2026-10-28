package com.company.booking.service;

import java.util.List;

/**
 * Thrown when a {@link com.company.booking.entity.Booking} violates the booking rules on save.
 */
public class BookingValidationException extends RuntimeException {

    private final List<String> messages;

    public BookingValidationException(final List<String> messages) {
        super(String.join("\n", messages));
        this.messages = List.copyOf(messages);
    }

    public List<String> getMessages() {
        return messages;
    }
}
