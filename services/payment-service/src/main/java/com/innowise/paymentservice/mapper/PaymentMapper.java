package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.PaymentEvent;
import com.innowise.paymentservice.dto.PaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps payments between entity, API and event representations.
 */
@Mapper
public interface PaymentMapper {

    /**
     * Creates a new payment from a request; identifier, status and timestamp are set by the
     * service.
     *
     * @param request create request
     * @return payment entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toEntity(PaymentRequest request);

    /**
     * Converts a payment to its API representation.
     *
     * @param payment payment entity
     * @return payment response
     */
    PaymentResponse toResponse(Payment payment);

    /**
     * Converts payments to their API representations.
     *
     * @param payments payment entities
     * @return payment responses
     */
    List<PaymentResponse> toResponses(List<Payment> payments);

    /**
     * Converts a payment to the event published to Kafka.
     *
     * @param payment payment entity
     * @return payment event
     */
    @Mapping(target = "paymentId", source = "id")
    PaymentEvent toEvent(Payment payment);
}
