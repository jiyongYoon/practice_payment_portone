package jy.test.persistence.impl;

import jy.test.entity.PaymentHistory;
import jy.test.persistence.PaymentHistoryJpaRepository;
import jy.test.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentHistoryRepositoryImpl implements PaymentHistoryRepository {

    private final PaymentHistoryJpaRepository jpaRepository;


    @Override
    public PaymentHistory save(PaymentHistory paymentHistory) {
        return jpaRepository.save(paymentHistory);
    }
}
