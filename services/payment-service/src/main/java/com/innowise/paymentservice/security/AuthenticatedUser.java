package com.innowise.paymentservice.security;

/**
 * Identity of the caller, taken from the headers set by the API Gateway.
 *
 * @param id user identifier
 * @param role user role
 */
public record AuthenticatedUser(Long id, UserRole role) {

    /**
     * Tells whether the caller is an administrator.
     *
     * @return {@code true} for administrators
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
