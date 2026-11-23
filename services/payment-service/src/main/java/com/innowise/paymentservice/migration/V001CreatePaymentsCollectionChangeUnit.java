package com.innowise.paymentservice.migration;

import com.innowise.paymentservice.entity.Payment;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * Creates the {@code payments} collection and the indexes required by the payment queries.
 *
 * <ul>
 *   <li>{@code user_id + timestamp} &ndash; payments of a user and per-user sums by date range;</li>
 *   <li>{@code order_id} &ndash; payments of an order;</li>
 *   <li>{@code status} &ndash; filtering by status;</li>
 *   <li>{@code timestamp} &ndash; sums by date range across all users (admin).</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@ChangeUnit(id = "v001-create-payments-collection", order = "001", author = "payment-service")
public class V001CreatePaymentsCollectionChangeUnit {

    private final MongoTemplate mongoTemplate;

    /**
     * Creates the collection (if missing) and its indexes.
     */
    @Execution
    public void execute() {
        if (!mongoTemplate.collectionExists(Payment.COLLECTION_NAME)) {
            mongoTemplate.createCollection(Payment.COLLECTION_NAME);
            log.info("Created collection '{}'", Payment.COLLECTION_NAME);
        }

        IndexOperations indexOps = mongoTemplate.indexOps(Payment.COLLECTION_NAME);
        indexOps.ensureIndex(new Index()
                .on(Payment.USER_ID_FIELD, Sort.Direction.ASC)
                .on(Payment.TIMESTAMP_FIELD, Sort.Direction.DESC)
                .named("idx_payments_user_id_timestamp"));
        indexOps.ensureIndex(new Index()
                .on(Payment.ORDER_ID_FIELD, Sort.Direction.ASC)
                .named("idx_payments_order_id"));
        indexOps.ensureIndex(new Index()
                .on(Payment.STATUS_FIELD, Sort.Direction.ASC)
                .named("idx_payments_status"));
        indexOps.ensureIndex(new Index()
                .on(Payment.TIMESTAMP_FIELD, Sort.Direction.DESC)
                .named("idx_payments_timestamp"));
        log.info("Created indexes for collection '{}'", Payment.COLLECTION_NAME);
    }

    /**
     * Drops the collection together with its indexes.
     */
    @RollbackExecution
    public void rollback() {
        mongoTemplate.dropCollection(Payment.COLLECTION_NAME);
        log.info("Dropped collection '{}'", Payment.COLLECTION_NAME);
    }
}
