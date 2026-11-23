package com.innowise.paymentservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.innowise.paymentservice.client.PaymentProviderClient;
import com.innowise.paymentservice.dto.PaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.dto.PaymentTotalResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.exception.ForbiddenException;
import com.innowise.paymentservice.exception.InvalidDateRangeException;
import com.innowise.paymentservice.exception.PaymentProviderException;
import com.innowise.paymentservice.mapper.PaymentMapperImpl;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.security.AuthenticatedUser;
import com.innowise.paymentservice.security.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String PAYMENT_ID = "payment-1";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ORDER_ID = 10L;
    private static final BigDecimal AMOUNT = new BigDecimal("100.50");
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-01-31T23:59:59Z");
    private static final AuthenticatedUser USER = new AuthenticatedUser(USER_ID, UserRole.USER);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(99L, UserRole.ADMIN);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProviderClient paymentProviderClient;

    private PaymentServiceImpl paymentService;

    /** Statuses the payment had at the moment of each {@code save} call. */
    private List<PaymentStatus> savedStatuses;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository, new PaymentMapperImpl(), paymentProviderClient);
        savedStatuses = new ArrayList<>();
    }

    @Test
    void createPayment_whenProviderApproves_savesSuccessfulPayment() {
        stubSave();
        when(paymentProviderClient.processPayment()).thenReturn(PaymentStatus.SUCCESS);
        Instant before = Instant.now();

        PaymentResponse response = paymentService.createPayment(request(USER_ID), USER);

        assertThat(response.id()).isEqualTo(PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.paymentAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(response.timestamp()).isBetween(before, Instant.now());
        assertThat(savedStatuses).containsExactly(PaymentStatus.PENDING, PaymentStatus.SUCCESS);
    }

    @Test
    void createPayment_whenProviderRejects_savesFailedPayment() {
        stubSave();
        when(paymentProviderClient.processPayment()).thenReturn(PaymentStatus.FAILED);

        PaymentResponse response = paymentService.createPayment(request(USER_ID), USER);

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedStatuses).containsExactly(PaymentStatus.PENDING, PaymentStatus.FAILED);
    }

    @Test
    void createPayment_whenProviderUnavailable_marksPaymentFailed() {
        stubSave();
        when(paymentProviderClient.processPayment())
                .thenThrow(new PaymentProviderException("provider is down"));

        PaymentResponse response = paymentService.createPayment(request(USER_ID), USER);

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedStatuses).containsExactly(PaymentStatus.PENDING, PaymentStatus.FAILED);
    }

    @Test
    void createPayment_whenUserPaysForAnotherUser_throwsForbidden() {
        PaymentRequest request = request(OTHER_USER_ID);

        assertThatThrownBy(() -> paymentService.createPayment(request, USER))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void createPayment_whenAdminPaysForAnotherUser_createsPayment() {
        stubSave();
        when(paymentProviderClient.processPayment()).thenReturn(PaymentStatus.SUCCESS);

        PaymentResponse response = paymentService.createPayment(request(OTHER_USER_ID), ADMIN);

        assertThat(response.userId()).isEqualTo(OTHER_USER_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void getPayments_whenUserOmitsUserId_returnsOwnPayments() {
        Set<PaymentStatus> statuses = Set.of(PaymentStatus.SUCCESS);
        when(paymentRepository.search(USER_ID, ORDER_ID, statuses))
                .thenReturn(List.of(payment(USER_ID)));

        List<PaymentResponse> result = paymentService.getPayments(null, ORDER_ID, statuses, USER);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(PAYMENT_ID);
        assertThat(result.getFirst().userId()).isEqualTo(USER_ID);
    }

    @Test
    void getPayments_whenUserRequestsOwnUserId_returnsOwnPayments() {
        when(paymentRepository.search(USER_ID, null, null)).thenReturn(List.of(payment(USER_ID)));

        List<PaymentResponse> result = paymentService.getPayments(USER_ID, null, null, USER);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPayments_whenUserRequestsAnotherUser_throwsForbidden() {
        assertThatThrownBy(() -> paymentService.getPayments(OTHER_USER_ID, null, null, USER))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getPayments_whenAdminOmitsUserId_searchesAllUsers() {
        when(paymentRepository.search(null, null, null))
                .thenReturn(List.of(payment(USER_ID), payment(OTHER_USER_ID)));

        List<PaymentResponse> result = paymentService.getPayments(null, null, null, ADMIN);

        assertThat(result).extracting(PaymentResponse::userId)
                .containsExactly(USER_ID, OTHER_USER_ID);
    }

    @Test
    void getPayments_whenAdminRequestsUser_searchesThatUser() {
        when(paymentRepository.search(OTHER_USER_ID, null, null))
                .thenReturn(List.of(payment(OTHER_USER_ID)));

        List<PaymentResponse> result =
                paymentService.getPayments(OTHER_USER_ID, null, null, ADMIN);

        assertThat(result).hasSize(1);
    }

    @Test
    void getTotal_whenUserOmitsUserId_returnsOwnTotal() {
        when(paymentRepository.sumSuccessfulPayments(USER_ID, FROM, TO)).thenReturn(AMOUNT);

        PaymentTotalResponse result = paymentService.getTotal(null, FROM, TO, USER);

        assertThat(result).isEqualTo(new PaymentTotalResponse(USER_ID, FROM, TO, AMOUNT));
    }

    @Test
    void getTotal_whenUserRequestsAnotherUser_throwsForbidden() {
        assertThatThrownBy(() -> paymentService.getTotal(OTHER_USER_ID, FROM, TO, USER))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getTotal_whenAdminOmitsUserId_returnsTotalOfAllUsers() {
        when(paymentRepository.sumSuccessfulPayments(null, FROM, TO)).thenReturn(AMOUNT);

        PaymentTotalResponse result = paymentService.getTotal(null, FROM, TO, ADMIN);

        assertThat(result).isEqualTo(new PaymentTotalResponse(null, FROM, TO, AMOUNT));
    }

    @Test
    void getTotal_whenAdminRequestsUser_returnsTotalOfThatUser() {
        when(paymentRepository.sumSuccessfulPayments(OTHER_USER_ID, FROM, TO))
                .thenReturn(BigDecimal.ZERO);

        PaymentTotalResponse result = paymentService.getTotal(OTHER_USER_ID, FROM, TO, ADMIN);

        assertThat(result.userId()).isEqualTo(OTHER_USER_ID);
        assertThat(result.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotal_whenFromEqualsTo_isAllowed() {
        when(paymentRepository.sumSuccessfulPayments(USER_ID, FROM, FROM)).thenReturn(AMOUNT);

        PaymentTotalResponse result = paymentService.getTotal(null, FROM, FROM, USER);

        assertThat(result.total()).isEqualByComparingTo(AMOUNT);
    }

    @Test
    void getTotal_whenFromIsAfterTo_throwsInvalidDateRange() {
        assertThatThrownBy(() -> paymentService.getTotal(null, TO, FROM, USER))
                .isInstanceOf(InvalidDateRangeException.class);

        verifyNoInteractions(paymentRepository);
    }

    /**
     * Makes {@code save} behave like MongoDB: assign an identifier and return the same object.
     */
    private void stubSave() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(PAYMENT_ID);
            }
            savedStatuses.add(payment.getStatus());
            return payment;
        });
    }

    private static PaymentRequest request(Long userId) {
        return new PaymentRequest(ORDER_ID, userId, AMOUNT);
    }

    private static Payment payment(Long userId) {
        return Payment.builder()
                .id(PAYMENT_ID)
                .orderId(ORDER_ID)
                .userId(userId)
                .status(PaymentStatus.SUCCESS)
                .timestamp(FROM)
                .paymentAmount(AMOUNT)
                .build();
    }
}