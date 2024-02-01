package jy.test.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import jy.test.dto.PaymentDto;
import jy.test.entity.Order;
import jy.test.entity.Payment;
import jy.test.entity.PaymentHistory;
import jy.test.entity.PaymentSchedule;
import jy.test.enumeration.*;
import jy.test.persistence.impl.PaymentScheduleRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegularPaymentScheduler {

    private final IamportClient iamportClient;
    private final PaymentService paymentService;
    private final PaymentScheduleService paymentScheduleService;
    private final PaymentScheduleRepositoryImpl paymentScheduleRepository;

    @Scheduled(cron = "1 * * * * *") // 매 분의 1초마다
    public void regularPaymentScheduling() {
        log.info("========== regularPaymentScheduling ==========");

        List<PaymentSchedule> paymentScheduleList =
                paymentScheduleRepository.findBootPaymentScheduleFetchOrderAndPayment(Instant.now());

        for (PaymentSchedule paymentSchedule : paymentScheduleList) {
            Order order = paymentSchedule.getOrder();
            Payment payment = order.getPayment();
            paymentScheduleService.activePaymentSchedule(order.getId(), SchedulingType.SPRINGBOOT);

            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
            try {
                iamportResponse = iamportClient.paymentByImpUid(payment.getPaymentUid());
            } catch (IamportResponseException | IOException e) {
                log.error("paymentByImpUid() exception.", e);
                throw new RuntimeException("paymentByImpUid() exception.", e);
            }

            // 이미 결제된 건
            if (iamportResponse.getResponse().getStatus().equals("paid")) {
                PaymentHistory savedPaymentHistory =
                        paymentService.savePaymentHistory(
                                payment.getPaymentUid(), order.getOrderUid(), PaymentCheckType.SCHEDULER, PaymentType.REGULAR);
                savedPaymentHistory.markHistoryType(PaymentHistoryType.CHECK);

                String paymentUid = iamportResponse.getResponse().getImpUid();
                Date paidAt = iamportResponse.getResponse().getPaidAt();

                // DB도 반영이 됐다면
                if (PaymentStatus.OK.equals(payment.getStatus())) {
                    log.info("already paid. paymentUid={}, paidAt={}", paymentUid, paidAt);
                }
                // DB 반영이 안됐다면
                else {
                    payment.changePaymentBySuccess(
                            paymentUid, PaymentCheckType.SCHEDULER, PaymentType.REGULAR, paidAt.toInstant());
                    log.info("already paid, but db did not update! check logic! paymentUid={}, paidAt={}",
                            paymentUid, paidAt);
                }
                return;
            }
            // 결제가 안됐으면 재결제 요청
            else {
                log.info("request by billingKey.");
                paymentService.paymentByBillingKey(PaymentDto.builder()
                        .paymentUid(payment.getPaymentUid())
                        .orderUid(order.getOrderUid())
                        .customerUid(order.getMember().getBillingKey())
                        .build());
                // TODO 또 안되는 경우도 체크해야함
            }
        }
    }
}
