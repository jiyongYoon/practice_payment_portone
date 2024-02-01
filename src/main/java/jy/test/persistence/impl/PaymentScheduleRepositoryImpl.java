package jy.test.persistence.impl;

import jy.test.entity.PaymentSchedule;
import jy.test.enumeration.SchedulingType;
import jy.test.persistence.PaymentScheduleJpaRepository;
import jy.test.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentScheduleRepositoryImpl implements PaymentScheduleRepository {

    private final PaymentScheduleJpaRepository jpaRepository;


    @Override
    public PaymentSchedule save(PaymentSchedule paymentSchedule) {
        return jpaRepository.save(paymentSchedule);
    }

    @Override
    public Optional<PaymentSchedule> findByOrderIdAndSchedulingType(Long orderId, SchedulingType schedulingType) {
        return jpaRepository.findByOrderIdAndSchedulingType(orderId, schedulingType);
    }

    @Override
    public Optional<PaymentSchedule> findById(Long paymentScheduleId) {
        return jpaRepository.findById(paymentScheduleId);
    }

    @Override
    public List<PaymentSchedule> findBootPaymentScheduleFetchOrderAndPayment(Instant now) {
        return jpaRepository.findBootPaymentScheduleFetchOrderAndPayment(now);
    }
}
