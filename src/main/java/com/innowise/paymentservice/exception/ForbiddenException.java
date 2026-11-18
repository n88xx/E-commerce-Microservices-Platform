package com.innowise.paymentservice.exception;

/**
 * Thrown when the caller is not allowed to access the requested resource.
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message detail message
     */
    public ForbiddenException(String message) {
        super(message);
    }
}
