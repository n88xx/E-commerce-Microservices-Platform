package com.innowise.paymentservice.security;

/**
 * Role of the caller, as resolved by the API Gateway.
 */
public enum UserRole {

    /** Full access to the payments of every user. */
    ADMIN,

    /** Access limited to the caller's own payments. */
    USER
}
