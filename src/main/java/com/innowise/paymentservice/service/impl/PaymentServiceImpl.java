package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.PaymentProviderClient;
import com.innowise.paymentservice.dto.PaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.dto.PaymentTotalResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.exception.ForbiddenException;
import com.innowise.paymentservice.exception.InvalidDateRangeException;
import com.innowise.paymentservice.exception.PaymentProviderException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.security.AuthenticatedUser;
import com.innowise.paymentservice.service.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default {@link PaymentService} implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentProviderClient paymentProviderClient;

    @Override
    public PaymentResponse createPayment(PaymentRequest request, AuthenticatedUser caller) {
        resolveUserId(request.userId(), caller);

        Payment payment = paymentMapper.toEntity(request);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(Instant.now());
        payment = paymentRepository.save(payment);

        payment.setStatus(requestStatus(payment));
        payment = paymentRepository.save(payment);
        log.info("Payment processed: id={}, orderId={}, status={}",
                payment.getId(), payment.getOrderId(), payment.getStatus());

        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPayments(
            Long userId, Long orderId, Set<PaymentStatus> statuses, AuthenticatedUser caller) {
        Long effectiveUserId = resolveUserId(userId, caller);
        return paymentMapper.toResponses(
                paymentRepository.search(effectiveUserId, orderId, statuses));
    }

    @Override
    public PaymentTotalResponse getTotal(
            Long userId, Instant from, Instant to, AuthenticatedUser caller) {
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("'from' must not be after 'to'");
        }
        Long effectiveUserId = resolveUserId(userId, caller);
        BigDecimal total = paymentRepository.sumSuccessfulPayments(effectiveUserId, from, to);
        return new PaymentTotalResponse(effectiveUserId, from, to, total);
    }

    /**
     * Asks the provider for the payment status; an unavailable provider fails the payment.
     */
    private PaymentStatus requestStatus(Payment payment) {
        try {
            return paymentProviderClient.processPayment();
        } catch (PaymentProviderException e) {
            log.error("Payment provider failed, marking payment as FAILED: id={}",
                    payment.getId(), e);
            return PaymentStatus.FAILED;
        }
    }

    /**
     * Returns the user the operation applies to. Administrators may target any user (or none);
     * other callers are limited to themselves.
     */
    private Long resolveUserId(Long requestedUserId, AuthenticatedUser caller) {
        if (caller.isAdmin()) {
            return requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.equals(caller.id())) {
            throw new ForbiddenException("Access to the payments of another user is denied");
        }
        return caller.id();
    }
}
