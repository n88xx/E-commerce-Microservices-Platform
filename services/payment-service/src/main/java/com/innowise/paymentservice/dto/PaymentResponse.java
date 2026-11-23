package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment representation returned by the API.
 *
 * @param id payment identifier
 * @param orderId identifier of the paid order
 * @param userId identifier of the paying user
 * @param status payment status
 * @param timestamp moment the payment was registered
 * @param paymentAmount paid amount
 */
public record PaymentResponse(
        String id,
        Long orderId,
        Long userId,
        PaymentStatus status,
        Instant timestamp,
        BigDecimal paymentAmount) {
}
