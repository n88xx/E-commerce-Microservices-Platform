package com.innowise.paymentservice.exception;

/**
 * Thrown when the identity of the caller cannot be established.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message detail message
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
