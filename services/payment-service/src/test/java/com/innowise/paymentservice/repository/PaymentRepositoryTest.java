package com.innowise.paymentservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataMongoTest
@Testcontainers
@Import(MongockTestConfig.class)
class PaymentRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final Instant JAN_10 = Instant.parse("2026-01-10T10:00:00Z");
    private static final Instant JAN_20 = Instant.parse("2026-01-20T10:00:00Z");
    private static final Instant FEB_10 = Instant.parse("2026-02-10T10:00:00Z");
    private static final Instant JAN_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant JAN_END = Instant.parse("2026-01-31T23:59:59Z");

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("payment_db"));
    }

    @BeforeEach
    void cleanUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void save_persistsPaymentAndGeneratesId() {
        Payment saved = paymentRepository.save(
                payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "100.50"));

        assertThat(saved.getId()).isNotBlank();
        assertThat(paymentRepository.findById(saved.getId())).hasValueSatisfying(found -> {
            assertThat(found.getUserId()).isEqualTo(USER_1);
            assertThat(found.getOrderId()).isEqualTo(10L);
            assertThat(found.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(found.getTimestamp()).isEqualTo(JAN_10);
            assertThat(found.getPaymentAmount()).isEqualByComparingTo("100.50");
        });
    }

    @Test
    void save_storesSnakeCaseFieldsAndDecimalAmount() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "100.50"));

        Document document = mongoTemplate.getCollection(Payment.COLLECTION_NAME).find().first();

        assertThat(document).isNotNull();
        assertThat((Long)document.get(Payment.USER_ID_FIELD)).isEqualTo(USER_1);
        assertThat((Long)document.get(Payment.ORDER_ID_FIELD)).isEqualTo(10L);
        assertThat((String)document.get(Payment.STATUS_FIELD)).isEqualTo("SUCCESS");
        assertThat(document.get(Payment.PAYMENT_AMOUNT_FIELD)).isInstanceOf(Decimal128.class);
    }

    @Test
    void migration_createsIndexes() {
        List<String> indexNames = mongoTemplate.indexOps(Payment.COLLECTION_NAME).getIndexInfo()
                .stream()
                .map(IndexInfo::getName)
                .toList();

        assertThat(indexNames).contains(
                "idx_payments_user_id_timestamp",
                "idx_payments_order_id",
                "idx_payments_status",
                "idx_payments_timestamp");
    }

    @Test
    void search_withoutCriteria_returnsAllNewestFirst() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_2, 20L, PaymentStatus.FAILED, FEB_10, "20.00"));
        paymentRepository.save(payment(USER_1, 30L, PaymentStatus.SUCCESS, JAN_20, "30.00"));

        List<Payment> result = paymentRepository.search(null, null, null);

        assertThat(result).extracting(Payment::getTimestamp)
                .containsExactly(FEB_10, JAN_20, JAN_10);
    }

    @Test
    void search_byUserId_returnsOnlyPaymentsOfThatUser() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_2, 20L, PaymentStatus.SUCCESS, JAN_10, "20.00"));

        List<Payment> result = paymentRepository.search(USER_1, null, Set.of());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(USER_1);
    }

    @Test
    void search_byOrderId_returnsOnlyPaymentsOfThatOrder() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.FAILED, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_20, "10.00"));
        paymentRepository.save(payment(USER_1, 20L, PaymentStatus.SUCCESS, JAN_10, "20.00"));

        List<Payment> result = paymentRepository.search(null, 10L, null);

        assertThat(result).hasSize(2).allMatch(payment -> payment.getOrderId().equals(10L));
    }

    @Test
    void search_byStatuses_returnsPaymentsWithAnyOfThem() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_1, 20L, PaymentStatus.FAILED, JAN_10, "20.00"));
        paymentRepository.save(payment(USER_1, 30L, PaymentStatus.PENDING, JAN_10, "30.00"));

        List<Payment> result = paymentRepository.search(
                null, null, Set.of(PaymentStatus.FAILED, PaymentStatus.PENDING));

        assertThat(result).extracting(Payment::getStatus)
                .containsExactlyInAnyOrder(PaymentStatus.FAILED, PaymentStatus.PENDING);
    }

    @Test
    void search_byAllCriteria_combinesThem() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.FAILED, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_2, 10L, PaymentStatus.SUCCESS, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_1, 20L, PaymentStatus.SUCCESS, JAN_10, "20.00"));

        List<Payment> result =
                paymentRepository.search(USER_1, 10L, Set.of(PaymentStatus.SUCCESS));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(USER_1);
        assertThat(result.getFirst().getOrderId()).isEqualTo(10L);
        assertThat(result.getFirst().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void sumSuccessfulPayments_forUser_sumsOnlySuccessfulPaymentsOfThatUserInRange() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "100.50"));
        paymentRepository.save(payment(USER_1, 20L, PaymentStatus.SUCCESS, JAN_20, "50.25"));
        paymentRepository.save(payment(USER_1, 30L, PaymentStatus.FAILED, JAN_20, "999.00"));
        paymentRepository.save(payment(USER_1, 40L, PaymentStatus.SUCCESS, FEB_10, "999.00"));
        paymentRepository.save(payment(USER_2, 50L, PaymentStatus.SUCCESS, JAN_10, "999.00"));

        BigDecimal total = paymentRepository.sumSuccessfulPayments(USER_1, JAN_START, JAN_END);

        assertThat(total).isEqualByComparingTo("150.75");
    }

    @Test
    void sumSuccessfulPayments_withoutUser_sumsOverAllUsers() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "100.50"));
        paymentRepository.save(payment(USER_2, 20L, PaymentStatus.SUCCESS, JAN_20, "50.25"));
        paymentRepository.save(payment(USER_2, 30L, PaymentStatus.FAILED, JAN_20, "999.00"));
        paymentRepository.save(payment(USER_2, 40L, PaymentStatus.SUCCESS, FEB_10, "999.00"));

        BigDecimal total = paymentRepository.sumSuccessfulPayments(null, JAN_START, JAN_END);

        assertThat(total).isEqualByComparingTo("150.75");
    }

    @Test
    void sumSuccessfulPayments_includesRangeBoundaries() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.SUCCESS, JAN_10, "10.00"));
        paymentRepository.save(payment(USER_1, 20L, PaymentStatus.SUCCESS, JAN_20, "20.00"));

        BigDecimal total = paymentRepository.sumSuccessfulPayments(USER_1, JAN_10, JAN_20);

        assertThat(total).isEqualByComparingTo("30.00");
    }

    @Test
    void sumSuccessfulPayments_whenNothingMatches_returnsZero() {
        paymentRepository.save(payment(USER_1, 10L, PaymentStatus.FAILED, JAN_10, "10.00"));

        BigDecimal total = paymentRepository.sumSuccessfulPayments(USER_1, JAN_START, JAN_END);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static Payment payment(
            Long userId, Long orderId, PaymentStatus status, Instant timestamp, String amount) {
        return Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .status(status)
                .timestamp(timestamp)
                .paymentAmount(new BigDecimal(amount))
                .build();
    }
}
