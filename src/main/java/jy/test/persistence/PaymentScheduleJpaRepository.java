package jy.test.persistence;

import jy.test.entity.PaymentSchedule;
import jy.test.enumeration.SchedulingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentScheduleJpaRepository extends JpaRepository<PaymentSchedule, Long> {

    Optional<PaymentSchedule> findByOrderIdAndSchedulingType(Long orderId, SchedulingType schedulingType);

    @Query("select ps from PaymentSchedule ps " +
            "left join fetch ps.order o " +
            "left join fetch o.payment p " +
            "where ps.schedulingType = jy.test.enumeration.SchedulingType.SPRINGBOOT " +
            "and ps.isDone is false " +
            "and ps.willSchedulingAt <= :now ")
    List<PaymentSchedule> findBootPaymentScheduleFetchOrderAndPayment(Instant now);
}
