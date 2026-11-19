package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for {@link Payment} documents.
 */
public interface PaymentRepository
        extends MongoRepository<Payment, String>, PaymentCustomRepository {
}
