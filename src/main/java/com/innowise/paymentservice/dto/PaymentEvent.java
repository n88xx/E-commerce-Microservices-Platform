package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published to Kafka once a payment has been processed.
 *
 * @param paymentId payment identifier
 * @param orderId identifier of the paid order
 * @param userId identifier of the paying user
 * @param status final payment status
 * @param paymentAmount paid amount
 * @param timestamp moment the payment was registered
 */
public record PaymentEvent(
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status,
        BigDecimal paymentAmount,
        Instant timestamp) {
}
