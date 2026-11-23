package com.innowise.paymentservice.client;

import com.innowise.paymentservice.config.PaymentProviderProperties;
import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.exception.PaymentProviderException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Payment provider simulated by a random number API: an even number approves the payment, an odd
 * number rejects it.
 */
@Slf4j
@Component
public class RandomNumberPaymentProviderClient implements PaymentProviderClient {

    private static final ParameterizedTypeReference<List<Integer>> NUMBERS_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final PaymentProviderProperties properties;

    /**
     * Creates the client.
     *
     * @param webClientBuilder builder pre-configured by Spring Boot
     * @param properties provider settings
     */
    public RandomNumberPaymentProviderClient(
            WebClient.Builder webClientBuilder, PaymentProviderProperties properties) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public PaymentStatus processPayment() {
        List<Integer> numbers;
        try {
            numbers = webClient.get()
                    .uri(properties.url())
                    .retrieve()
                    .bodyToMono(NUMBERS_TYPE)
                    .block(properties.timeout());
        } catch (RuntimeException e) {
            throw new PaymentProviderException("Payment provider request failed", e);
        }

        if (numbers == null || numbers.isEmpty() || numbers.getFirst() == null) {
            throw new PaymentProviderException("Payment provider returned an empty answer");
        }
        int number = numbers.getFirst();
        log.debug("Payment provider answered {}", number);
        return number % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}
