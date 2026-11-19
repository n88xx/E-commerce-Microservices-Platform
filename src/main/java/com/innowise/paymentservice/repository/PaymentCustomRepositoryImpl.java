package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * {@link MongoTemplate}-based implementation of {@link PaymentCustomRepository}.
 *
 * <p>Queries use entity property names; Spring Data maps them to the stored field names.
 */
@RequiredArgsConstructor
public class PaymentCustomRepositoryImpl implements PaymentCustomRepository {

    private static final String USER_ID = "userId";
    private static final String ORDER_ID = "orderId";
    private static final String STATUS = "status";
    private static final String TIMESTAMP = "timestamp";
    private static final String PAYMENT_AMOUNT = "paymentAmount";
    private static final String TOTAL = "total";

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Payment> search(Long userId, Long orderId, Collection<PaymentStatus> statuses) {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, TIMESTAMP));
        if (userId != null) {
            query.addCriteria(Criteria.where(USER_ID).is(userId));
        }
        if (orderId != null) {
            query.addCriteria(Criteria.where(ORDER_ID).is(orderId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            query.addCriteria(Criteria.where(STATUS).in(statuses));
        }
        return mongoTemplate.find(query, Payment.class);
    }

    @Override
    public BigDecimal sumSuccessfulPayments(Long userId, Instant from, Instant to) {
        Criteria criteria = Criteria.where(STATUS).is(PaymentStatus.SUCCESS)
                .and(TIMESTAMP).gte(from).lte(to);
        if (userId != null) {
            criteria = criteria.and(USER_ID).is(userId);
        }

        TypedAggregation<Payment> aggregation = Aggregation.newAggregation(
                Payment.class,
                Aggregation.match(criteria),
                Aggregation.group().sum(PAYMENT_AMOUNT).as(TOTAL));

        TotalResult result = mongoTemplate.aggregate(aggregation, TotalResult.class)
                .getUniqueMappedResult();
        return result == null || result.total() == null ? BigDecimal.ZERO : result.total();
    }

    /**
     * Projection of the {@code $group} stage result.
     *
     * @param total sum of the matched payment amounts
     */
    private record TotalResult(BigDecimal total) {
    }
}
