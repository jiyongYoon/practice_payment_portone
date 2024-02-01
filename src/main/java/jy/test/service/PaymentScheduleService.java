package jy.test.service;

import jy.test.entity.PaymentSchedule;
import jy.test.enumeration.SchedulingType;
import jy.test.persistence.impl.PaymentScheduleRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentScheduleService {

    private final PaymentScheduleRepositoryImpl paymentScheduleRepository;

    public void activePaymentSchedule(Long orderId, SchedulingType schedulingType) {
        PaymentSchedule paymentSchedule =
                paymentScheduleRepository.findByOrderIdAndSchedulingType(orderId, schedulingType)
                        .orElseThrow(NoSuchElementException::new);
        paymentSchedule.done();
    }
}
