package com.innowise.paymentservice.exception;

/**
 * Thrown when the external payment provider cannot be reached or returns an unusable answer.
 */
public class PaymentProviderException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message detail message
     */
    public PaymentProviderException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message detail message
     * @param cause underlying failure
     */
    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
