package com.innowise.paymentservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * Payment document stored in the {@value #COLLECTION_NAME} MongoDB collection.
 *
 * <p>Indexes for this collection are managed by Mongock migrations
 * (see {@code com.innowise.paymentservice.migration}), not by mapping annotations.
 */
@Document(collection = Payment.COLLECTION_NAME)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Payment {

    /** Name of the MongoDB collection that stores payments. */
    public static final String COLLECTION_NAME = "payments";

    /** Field name of the order identifier. */
    public static final String ORDER_ID_FIELD = "order_id";

    /** Field name of the user identifier. */
    public static final String USER_ID_FIELD = "user_id";

    /** Field name of the payment status. */
    public static final String STATUS_FIELD = "status";

    /** Field name of the payment timestamp. */
    public static final String TIMESTAMP_FIELD = "timestamp";

    /** Field name of the payment amount. */
    public static final String PAYMENT_AMOUNT_FIELD = "payment_amount";

    @Id
    private String id;

    @Field(ORDER_ID_FIELD)
    private Long orderId;

    @Field(USER_ID_FIELD)
    private Long userId;

    @Field(STATUS_FIELD)
    private PaymentStatus status;

    @Field(TIMESTAMP_FIELD)
    private Instant timestamp;

    /** Stored as Decimal128 so it can be summed precisely in aggregation pipelines. */
    @Field(name = PAYMENT_AMOUNT_FIELD, targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;
}
