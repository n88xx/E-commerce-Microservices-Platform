package com.innowise.paymentservice.client;

import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.exception.PaymentProviderException;

/**
 * Client of the external payment provider.
 */
public interface PaymentProviderClient {

    /**
     * Asks the provider to process a payment.
     *
     * @return {@link PaymentStatus#SUCCESS} or {@link PaymentStatus#FAILED}
     * @throws PaymentProviderException when the provider is unavailable or its answer is unusable
     */
    PaymentStatus processPayment();
}
