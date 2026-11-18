package com.innowise.paymentservice.entity;

/**
 * Lifecycle status of a payment.
 */
public enum PaymentStatus {

    /** Payment has been registered but the provider has not answered yet. */
    PENDING,

    /** Payment was confirmed by the payment provider. */
    SUCCESS,

    /** Payment was rejected by the payment provider. */
    FAILED
}
