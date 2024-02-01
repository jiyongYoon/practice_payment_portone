package jy.test.repository;

import jy.test.entity.PaymentSchedule;
import jy.test.enumeration.SchedulingType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentScheduleRepository {

    PaymentSchedule save(PaymentSchedule paymentSchedule);
    Optional<PaymentSchedule> findByOrderIdAndSchedulingType(Long orderId, SchedulingType schedulingType);
    Optional<PaymentSchedule> findById(Long paymentScheduleId);
    List<PaymentSchedule> findBootPaymentScheduleFetchOrderAndPayment(Instant now);
}
