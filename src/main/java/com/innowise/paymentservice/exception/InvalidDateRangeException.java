package com.innowise.paymentservice.exception;

/**
 * Thrown when the start of a requested period is after its end.
 */
public class InvalidDateRangeException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message detail message
     */
    public InvalidDateRangeException(String message) {
        super(message);
    }
}
