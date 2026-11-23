package com.innowise.paymentservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the external payment provider.
 *
 * @param url endpoint that returns a JSON array with a single random number
 * @param timeout maximum time to wait for the answer of the provider
 */
@Validated
@ConfigurationProperties(prefix = "payment.provider")
public record PaymentProviderProperties(@NotBlank String url, @NotNull Duration timeout) {
}
