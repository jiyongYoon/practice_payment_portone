package jy.test.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.ScheduleData;
import com.siot.IamportRestClient.request.ScheduleEntry;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Schedule;
import jy.test.bo.SchedulingEvent;
import jy.test.entity.Member;
import jy.test.entity.Order;
import jy.test.entity.PaymentSchedule;
import jy.test.enumeration.SchedulingType;
import jy.test.persistence.impl.MemberRepositoryImpl;
import jy.test.persistence.impl.OrderRepositoryImpl;
import jy.test.persistence.impl.PaymentScheduleRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduleListener {

    private final IamportClient iamportClient;
    private final MemberRepositoryImpl memberRepository;
    private final OrderRepositoryImpl orderRepository;
    private final PaymentScheduleRepositoryImpl paymentScheduleRepository;
    private final ChronoUnit nextPaymentUnit = ChronoUnit.SECONDS;
    private final long nextPaymentAmount = 60;
    private final long oneMinute = 60; // seconds

    @Value("${ngrok.port-forwarding}")
    private String ngrokPortForwarding;

    @Async(value = "asyncExecutor1")
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void registerNextPaymentScheduling(SchedulingEvent event) {
        SchedulingType schedulingType = event.getType();
        switch (schedulingType) {
            case PORT_ONE:
                registerPortOneScheduling(event);
                break;
            case SPRINGBOOT:
                registerBootScheduling(event);
                break;
        }
    }

    /**
     * 부트 스케쥴링 이벤트 등록 메서드
     */
    private void registerBootScheduling(SchedulingEvent event) {
        log.info("register boot scheduling. orderUid={}", event.getNextOrder().getOrderUid());
        Member member = memberRepository.findById(event.getMember().getId())
                .orElseThrow(NoSuchElementException::new);
        member.validateBillingIssued();

        Order nextOrder = orderRepository.findOrderFetchPaymentAndMemberForUpdate(event.getNextOrder().getOrderUid())
                .orElseThrow(NoSuchElementException::new);
        Instant orderedAt = nextOrder.getOrderedAt();
        Instant willPayAt = LocalDateTime.ofInstant(orderedAt, ZoneId.systemDefault())
                .plus(nextPaymentAmount + oneMinute, nextPaymentUnit)
                .withSecond(0)
                .atZone(ZoneId.systemDefault()).toInstant();

        nextOrder.registerSchedulingPayment(willPayAt);

        PaymentSchedule paymentSchedule = PaymentSchedule.builder()
                .schedulingType(SchedulingType.SPRINGBOOT)
                .willSchedulingAt(willPayAt)
                .order(nextOrder)
                .build();
        paymentScheduleRepository.save(paymentSchedule);

        log.info("내부 스케쥴링 등록 완료, willPayAt = {}", willPayAt);
    }

    /**
     * 포트원 스케쥴링 이벤트 등록 메서드 <br>
     */
    private void registerPortOneScheduling(SchedulingEvent event) {
        log.info("register PortOne scheduling. orderUid={}", event.getNextOrder().getOrderUid());
        Member member = memberRepository.findById(event.getMember().getId())
                .orElseThrow(NoSuchElementException::new);
        member.validateBillingIssued();

        Order nextOrder = orderRepository.findOrderFetchPaymentAndMemberForUpdate(event.getNextOrder().getOrderUid())
                .orElseThrow(NoSuchElementException::new);
        Instant orderedAt = nextOrder.getOrderedAt();
        Instant willPayAt = LocalDateTime.ofInstant(orderedAt, ZoneId.systemDefault())
                .plus(nextPaymentAmount, nextPaymentUnit)
                .withSecond(0)
                .atZone(ZoneId.systemDefault()).toInstant();

        ScheduleData scheduleData = new ScheduleData(member.getBillingKey());
        ScheduleEntry scheduleEntry =
                new ScheduleEntry(
                        nextOrder.getOrderUid(),
                        new Date(willPayAt.toEpochMilli()),
                        new BigDecimal(nextOrder.getPayment().getPrice()));
        scheduleEntry.setNoticeUrl(ngrokPortForwarding + "/payment/webhook/schedule");
        scheduleData.addSchedule(scheduleEntry);
        IamportResponse<List<Schedule>> listIamportResponse = new IamportResponse<>();
        try {
            listIamportResponse = iamportClient.subscribeSchedule(scheduleData);
        } catch (IamportResponseException | IOException e) {
            throw new RuntimeException(e);
        }

        // 정기결제 등록완료
        if (listIamportResponse.getResponse() != null && listIamportResponse.getCode() == 0) {
            nextOrder.registerSchedulingPayment(willPayAt);

            PaymentSchedule paymentSchedule = PaymentSchedule.builder()
                    .schedulingType(SchedulingType.PORT_ONE)
                    .willSchedulingAt(willPayAt)
                    .order(nextOrder)
                    .build();
            paymentScheduleRepository.save(paymentSchedule);

            log.info("포트원 정기결제 등록 완료, willPayAt={}", willPayAt);
        } else {
            System.out.println("listIamportResponse.getCode() = " + listIamportResponse.getCode());
            System.out.println("listIamportResponse.getMessage() = " + listIamportResponse.getMessage());
            System.out.println("listIamportResponse.getResponse() = " + listIamportResponse.getResponse());
            log.warn("포트원 정기결제 등록 실패 (부트 스캐쥴링이 잘 되었는지 확인!, 나중에는 포트원에 등록 안된거 스케쥴링하여 다시 등록하도록 해야할 것)");
            // TODO 만약 부트 스케쥴링도 실패하면 정기결제 건을 애초에 취소해야할수도 있음.
        }
    }
}
