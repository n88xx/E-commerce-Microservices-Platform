package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Custom payment queries that cannot be expressed as derived repository methods.
 */
public interface PaymentCustomRepository {

    /**
     * Finds payments matching every non-empty criterion, newest first.
     *
     * @param userId user identifier, or {@code null} for any user
     * @param orderId order identifier, or {@code null} for any order
     * @param statuses statuses to match, or {@code null}/empty for any status
     * @return matching payments ordered by timestamp descending
     */
    List<Payment> search(Long userId, Long orderId, Collection<PaymentStatus> statuses);

    /**
     * Sums the amounts of successful payments made within the given period.
     *
     * @param userId user identifier, or {@code null} to sum over all users
     * @param from start of the period, inclusive
     * @param to end of the period, inclusive
     * @return total amount, {@link BigDecimal#ZERO} when nothing matches
     */
    BigDecimal sumSuccessfulPayments(Long userId, Instant from, Instant to);
}
