package com.innowise.paymentservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Total amount of successful payments for a period.
 *
 * @param userId user the total belongs to, or {@code null} when summed over all users
 * @param from start of the period, inclusive
 * @param to end of the period, inclusive
 * @param total sum of successful payment amounts
 */
public record PaymentTotalResponse(Long userId, Instant from, Instant to, BigDecimal total) {
}
