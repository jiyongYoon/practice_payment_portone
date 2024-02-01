package jy.test.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import jy.test.bo.SchedulingEvent;
import jy.test.dto.PaymentCallbackRequest;
import jy.test.dto.PaymentDto;
import jy.test.dto.RequestPayDto;
import jy.test.entity.Member;
import jy.test.entity.Order;
import jy.test.entity.Payment;
import jy.test.entity.PaymentHistory;
import jy.test.enumeration.*;
import jy.test.persistence.impl.MemberRepositoryImpl;
import jy.test.persistence.impl.OrderRepositoryImpl;
import jy.test.persistence.impl.PaymentHistoryRepositoryImpl;
import jy.test.persistence.impl.PaymentRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderService orderService;
    private final PaymentScheduleService paymentScheduleService;
    private final OrderRepositoryImpl orderRepository;
    private final PaymentRepositoryImpl paymentRepository;
    private final MemberRepositoryImpl memberRepository;
    private final PaymentHistoryRepositoryImpl paymentHistoryRepository;
    private final IamportClient iamportClient;
    private final ApplicationEventPublisher eventPublisher;
    @Value("${ngrok.port-forwarding}")
    private String ngrokPortForwarding;

    /**
     * DB 결제정보를 가지고 클라이언트의 View 리턴
     */
    public RequestPayDto findRequestDto(String orderUid) {
        Order order = orderRepository.findOrderFetchPaymentAndMember(orderUid)
                .orElseThrow(NoSuchElementException::new);

        return RequestPayDto.builder()
                .orderUid(order.getOrderUid())
                .itemName(order.getItemName())
                .buyerName(order.getMember().getUsername())
                .paymentPrice(order.getPayment().getPrice())
                .buyerEmail(order.getMember().getEmail())
                .buyerAddress(order.getMember().getAddress())
                .customerUid(order.getMember().getBillingKey())
                .build();
    }

    /**
     * 결제정보 검증 후 어플리케이션 DB 결제완료로 변경 (Payment, PaymentHistory 업데이트) <br>
     * 이 메서드는 결제요청 후 검증용 웹훅으로만 호출하자!
     */
    public IamportResponse<com.siot.IamportRestClient.response.Payment> validatePayment(
            PaymentCallbackRequest request,
            PaymentCheckType paymentCheckType) {
        PaymentHistory savedPaymentHistory =
                savePaymentHistory(request.getPaymentUid(), request.getOrderUid(), paymentCheckType, request.getPaymentType());

        try {
            // 결제 단건 조회
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    getIamportResponse(request);

            Order order = orderRepository.findOrderFetchPaymentAndMemberForUpdate(request.getOrderUid())
                    .orElseThrow(NoSuchElementException::new);

            validatePaymentAndCancelIfAmountDifferent(iamportResponse, order);

            Payment payment = order.getPayment();
            if (PaymentStatus.OK.equals(payment.getStatus())) {
                savedPaymentHistory.markHistoryType(PaymentHistoryType.CHECK);
                log.info("이미 결제 완료된 주문입니다. payment_uid={}, order_uid={}",
                        payment.getPaymentUid(), order.getOrderUid());
            } else {
                savedPaymentHistory.markHistoryType(PaymentHistoryType.PAY);
                payment.changePaymentBySuccess(
                        iamportResponse.getResponse().getImpUid(),
                        paymentCheckType,
                        request.getPaymentType(),
                        iamportResponse.getResponse().getPaidAt().toInstant());
                log.info("{}으로 결제 완료 확인!, payment_uid={}, order_uid={}",
                        paymentCheckType + "-" + (request.getPaymentType() != null ? request.getPaymentType().toString() : "null"),
                        payment.getPaymentUid(), order.getOrderUid());
            }

            return iamportResponse;

        } catch (IamportResponseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 최초 정기결제 요청시 빌링키 발급여부를 저장하고 결제 진행
     */
    public void issuedBillingKey(PaymentCallbackRequest request) {
        Member member = memberRepository.findByBillingKey(request.getCustomerUid())
                .orElseThrow(NoSuchElementException::new);
        member.billingIssued();

        // 웹훅이 미리와서 처리는 하지만, 콜백으로 한번 더 체크 필요
        validatePayment(request, PaymentCheckType.ISSUED_BILLING_KEY);

        createNextOrderAndPublishSchedulingEvent(member);
    }

    /**
     * PortOne 스케쥴링 웹훅으로 결제가 된 경우, 다음 결제를 등록하는 메서드
     */
    public void paymentByIamportScheduling(PaymentCallbackRequest paymentCallbackRequest) {
        Order order = orderRepository.findOrderFetchPaymentAndMemberForUpdate(paymentCallbackRequest.getOrderUid())
                .orElseThrow(NoSuchElementException::new);

        paymentScheduleService.activePaymentSchedule(order.getId(), SchedulingType.PORT_ONE);

        createNextOrderAndPublishSchedulingEvent(order.getMember());
    }

    /**
     * PortOne 스케쥴링 웹훅으로 결제가 되지 않았을 경우 Springboot 스케쥴러가 사용되는 메서드
     */
    public void paymentByBillingKey(PaymentDto paymentDto) {
        PaymentHistory savedPaymentHistory =
                savePaymentHistory(paymentDto.getPaymentUid(), paymentDto.getOrderUid(), PaymentCheckType.SCHEDULER, PaymentType.REGULAR);

        RequestPayDto requestDto = findRequestDto(paymentDto.getOrderUid());

        AgainPaymentData againPaymentData = new AgainPaymentData(
                requestDto.getCustomerUid(),
                requestDto.getOrderUid(),
                new BigDecimal(requestDto.getPaymentPrice()));
        againPaymentData.setName(requestDto.getItemName());
        log.info("requestDto.getItemName={}", requestDto.getItemName());
        againPaymentData.setNoticeUrl(ngrokPortForwarding + "/payment/webhook/again");
        IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
        try {
            iamportResponse = iamportClient.againPayment(againPaymentData);
        } catch (IamportResponseException | IOException e) {
            log.error("againPayment() exception.", e);
            throw new RuntimeException("againPayment() exception.", e);
        }

        Instant paidAt = iamportResponse.getResponse().getPaidAt().toInstant();
        String paymentUid = iamportResponse.getResponse().getImpUid();
        String message = iamportResponse.getMessage();
        if (iamportResponse.getCode() == 0) { // 카드사 통신 성공
            log.info("responseCode == 0");
            //TODO response status에 대해 분기처리 상황 확인 필요
            switch (iamportResponse.getResponse().getStatus()) {
                case "paid":
                    savedPaymentHistory.updateHistoryInfo(paymentUid, PaymentHistoryType.PAY, message);

                    Order order = orderRepository.findOrderFetchPaymentAndMember(iamportResponse.getResponse().getMerchantUid())
                            .orElseThrow(NoSuchElementException::new);
                    order.getPayment().changePaymentBySuccess(
                            paymentUid, PaymentCheckType.SCHEDULER, PaymentType.REGULAR, paidAt);

                    log.info("paid! paymentUid={}, paidAt={}", paymentUid, paidAt);
                    break;
                case "failed":
                    savedPaymentHistory.updateHistoryInfo(paymentUid, PaymentHistoryType.CHECK, message);

                    log.info("failed! paymentUid={}, paidAt={}", paymentUid, paidAt);
                    break;
            }
        } else {
            savedPaymentHistory.updateHistoryInfo(paymentUid, PaymentHistoryType.CHECK, message);

            log.warn("responseCode={}. message={}", iamportResponse.getCode(), message);
        }
    }

    public PaymentHistory savePaymentHistory(String paymentUid,
                                             String orderUid,
                                             PaymentCheckType paymentCheckType,
                                             PaymentType paymentType) {
        PaymentHistory paymentHistory = PaymentHistory.builder()
                .paymentUid(paymentUid)
                .orderUid(orderUid)
                .createdAt(Instant.now())
                .paymentCheckType(paymentCheckType)
                .paymentType(paymentType)
                .build();
        return paymentHistoryRepository.save(paymentHistory);
    }

    /**
     * 다음 정기결제 주문 생성 및 이벤트 발행
     */
    private void createNextOrderAndPublishSchedulingEvent(Member member) {
        // 다음 주문 생성
        Instant now = Instant.now();
        Order nextOrder = orderService.autoOrder(member, "다음달 정기결제건", now);

        //다음 정기결제 일정 스케쥴 등록하기(포트원, 부트서버)
        eventPublisher.publishEvent(new SchedulingEvent(nextOrder, member, SchedulingType.PORT_ONE));
        eventPublisher.publishEvent(new SchedulingEvent(nextOrder, member, SchedulingType.SPRINGBOOT));
    }

    /**
     * 포트원으로부터 받아온 결제 데이터를 가지고 검증하는 메서드. <br>
     * 결제 완료가 되지 않음 -> 주문정보 및 결제정보 삭제 <br>
     * 결제 완료는 되었으나 결제 금액이 다름 -> 주문정보 및 결제정보 삭제 후, 포트원에 결제 취소 요청
     * @param iamportResponse 포트원으로부터 받은 결제 데이터
     * @param order 고객의 주문 정보
     */
    private void validatePaymentAndCancelIfAmountDifferent(
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse,
            Order order)
            throws IamportResponseException, IOException {

        // 결제 완료가 아님
        if (!iamportResponse.getResponse().getStatus().equals("paid")) {
            orderRepository.delete(order);
            paymentRepository.delete(order.getPayment());

            throw new RuntimeException("결제 미완료");
        }

        Long price = order.getPrice();
        long iamportPrice = iamportResponse.getResponse().getAmount().longValue();

        // 결제 금액 검증
        if (!Objects.equals(iamportPrice, price)) {
            orderRepository.delete(order);
            paymentRepository.delete(order.getPayment());

            CancelData cancelData = new CancelData(
                    iamportResponse.getResponse().getImpUid(),
                    true,
                    new BigDecimal(iamportPrice));

            iamportClient.cancelPaymentByImpUid(cancelData);

            throw new RuntimeException("결제금액 상이하여 취소, 클라이언트 측의 위변조 가능성 있음");
        }
    }

    private IamportResponse<com.siot.IamportRestClient.response.Payment> getIamportResponse(
            PaymentCallbackRequest request)
            throws IamportResponseException, IOException {

        IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                iamportClient.paymentByImpUid(request.getPaymentUid());
        return iamportResponse;
    }
}
