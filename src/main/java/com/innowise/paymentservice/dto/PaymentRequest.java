package com.innowise.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request to create a payment.
 *
 * @param orderId identifier of the order being paid
 * @param userId identifier of the paying user
 * @param paymentAmount amount to pay
 */
public record PaymentRequest(
        @NotNull(message = "orderId is required")
        @Positive(message = "orderId must be positive")
        Long orderId,

        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId,

        @NotNull(message = "paymentAmount is required")
        @DecimalMin(value = "0.01", message = "paymentAmount must be at least 0.01")
        @Digits(integer = 17, fraction = 2,
                message = "paymentAmount must have at most 17 integer and 2 fraction digits")
        BigDecimal paymentAmount) {
}
