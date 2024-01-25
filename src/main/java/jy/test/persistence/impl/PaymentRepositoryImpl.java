package jy.test.persistence.impl;

import jy.test.entity.Payment;
import jy.test.persistence.PaymentJpaRepository;
import jy.test.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;


    @Override
    public Payment save(Payment payment) {
        return jpaRepository.save(payment);
    }

    @Override
    public void delete(Payment payment) {
        jpaRepository.delete(payment);
    }
}
