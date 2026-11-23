package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.PaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.dto.PaymentTotalResponse;
import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.exception.ForbiddenException;
import com.innowise.paymentservice.exception.InvalidDateRangeException;
import com.innowise.paymentservice.security.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Payment business operations.
 *
 * <p>Administrators can work with the payments of any user; other callers only with their own.
 */
public interface PaymentService {

    /**
     * Registers a payment, asks the payment provider to process it and publishes the result.
     *
     * @param request payment to create
     * @param caller authenticated caller
     * @return processed payment
     * @throws ForbiddenException when a non-admin caller pays on behalf of another user
     */
    PaymentResponse createPayment(PaymentRequest request, AuthenticatedUser caller);

    /**
     * Finds payments by optional criteria, newest first.
     *
     * @param userId user identifier; non-admin callers default to their own
     * @param orderId order identifier, or {@code null} for any order
     * @param statuses statuses to match, or {@code null}/empty for any status
     * @param caller authenticated caller
     * @return matching payments
     * @throws ForbiddenException when a non-admin caller requests the payments of another user
     */
    List<PaymentResponse> getPayments(
            Long userId, Long orderId, Set<PaymentStatus> statuses, AuthenticatedUser caller);

    /**
     * Sums successful payments made within a period.
     *
     * @param userId user identifier; {@code null} means all users for administrators and the
     *     caller for everyone else
     * @param from start of the period, inclusive
     * @param to end of the period, inclusive
     * @param caller authenticated caller
     * @return total amount for the period
     * @throws ForbiddenException when a non-admin caller requests the total of another user
     * @throws InvalidDateRangeException when {@code from} is after {@code to}
     */
    PaymentTotalResponse getTotal(Long userId, Instant from, Instant to, AuthenticatedUser caller);
}
