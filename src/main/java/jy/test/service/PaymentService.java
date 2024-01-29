package jy.test.service;

import com.google.gson.JsonObject;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import jy.test.dto.PaymentCallbackRequest;
import jy.test.dto.PaymentDto;
import jy.test.dto.RequestPayDto;
import jy.test.entity.Member;
import jy.test.entity.Order;
import jy.test.entity.Payment;
import jy.test.entity.PaymentHistory;
import jy.test.enumeration.PaymentCheckType;
import jy.test.enumeration.PaymentHistoryType;
import jy.test.enumeration.PaymentStatus;
import jy.test.enumeration.PaymentType;
import jy.test.persistence.impl.MemberRepositoryImpl;
import jy.test.persistence.impl.OrderRepositoryImpl;
import jy.test.persistence.impl.PaymentHistoryRepositoryImpl;
import jy.test.persistence.impl.PaymentRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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

    private final PaymentScheduleService paymentScheduleService;
    private final OrderRepositoryImpl orderRepository;
    private final PaymentRepositoryImpl paymentRepository;
    private final MemberRepositoryImpl memberRepository;
    private final PaymentHistoryRepositoryImpl paymentHistoryRepository;
    private final IamportClient iamportClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${key.api}")
    private String apiKey;
    @Value("${key.secret}")
    private String secretKey;

    /**
     * DB 결제정보를 가지고 클라이언트의 View 리턴
     */
    public RequestPayDto findRequestDto(String orderUid) {
        Order order = orderRepository.findOrderFetchPaymentAndMember(orderUid)
                .orElseThrow(NoSuchElementException::new);

        return RequestPayDto.builder()
                .buyerName(order.getMember().getUsername())
                .buyerEmail(order.getMember().getEmail())
                .buyerAddress(order.getMember().getAddress())
                .paymentPrice(order.getPayment().getPrice())
                .itemName(order.getItemName())
                .orderUid(order.getOrderUid())
                .customerUid(order.getMember().getBillingKey())
                .build();
    }

    /**
     * 결제정보 검증 후 어플리케이션 DB 결제완료로 변경
     */
    public IamportResponse<com.siot.IamportRestClient.response.Payment> validatePayment(
            PaymentCallbackRequest request,
            PaymentCheckType paymentCheckType) {
        PaymentHistory savedPaymentHistory = savePaymentHistory(request, paymentCheckType, request.getPaymentType());

        try {
            // 결제 단건 조회
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    getIamportResponse(request);

            Order order = orderRepository.findOrderFetchPayment(request.getOrderUid())
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
                        PaymentStatus.OK, iamportResponse.getResponse().getImpUid(), paymentCheckType, request.getPaymentType());
                log.info("{}으로 결제 완료!, payment_uid={}, order_uid={}",
                        paymentCheckType + "-" + (request.getPaymentType() != null ? request.getPaymentType().toString() : "null")
                        , payment.getPaymentUid(), order.getOrderUid());
            }

            return iamportResponse;

        } catch (IamportResponseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 정기결제 요청시 빌링키 발급여부를 저장하고 결제 진행
     */
    public void issuedBillingKey(PaymentCallbackRequest request) {
        Member member = memberRepository.findByBillingKey(request.getCustomerUid())
                .orElseThrow(NoSuchElementException::new);
        member.billingIssued();

        // 웹훅이 미리와서 처리는 하지만, 콜백으로 한번 더 체크 필요
        validatePayment(request, PaymentCheckType.ISSUED_BILLING_KEY);

        //TODO 다음 정기결제 일정 스케쥴 등록하기
        paymentScheduleService.makeNextOrderAndReserve(member);
    }

    public void paymentByBillingKey(PaymentDto paymentDto) throws IamportResponseException, IOException {
        String token = iamportClient.getAuth().getResponse().getToken();
        System.out.println("token = " + token);

        RequestPayDto requestDto = findRequestDto(paymentDto.getOrderUid());

        String url = "https://api.iamport.kr/subscribe/payments/again";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, token);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("customer_uid", paymentDto.getCustomerUid()); // required
        jsonObject.addProperty("merchant_uid", requestDto.getOrderUid()); // required
        jsonObject.addProperty("name", requestDto.getItemName()); // required
        jsonObject.addProperty("amount", requestDto.getPaymentPrice()); // required
        jsonObject.addProperty("buyer_email", requestDto.getBuyerEmail());
        jsonObject.addProperty("buyer_name", requestDto.getBuyerName());
        jsonObject.addProperty("buyer_addr", requestDto.getBuyerAddress());
        jsonObject.addProperty("buyer_addr", requestDto.getBuyerAddress());

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);

        ResponseEntity<IamportResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, IamportResponse.class);
        // 카드사 통신 성공시 IamportResponse<Payment>, 실패시 IamportResponse<PaymentCancelDetail>

        int responseCode = responseEntity.getBody().getCode();

        if (responseCode == 0) { // 카드사 통신 성공
            log.info("카드가 정상적으로 조회됐습니다.");
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportSuccessResponse =
                    (IamportResponse<com.siot.IamportRestClient.response.Payment>) responseEntity.getBody();

            switch (iamportSuccessResponse.getResponse().getStatus()) {
                case "paid" :
                case "failed" :
                    break;
            }
        } else {
            log.warn("카드가 정상적으로 조회되지 않았습니다. message={}", responseEntity.getBody().getMessage());
        }
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

    private PaymentHistory savePaymentHistory(PaymentCallbackRequest request,
                                    PaymentCheckType paymentCheckType,
                                    PaymentType paymentType) {
        PaymentHistory paymentHistory = PaymentHistory.builder()
                .paymentUid(request.getPaymentUid())
                .orderUid(request.getOrderUid())
                .createdAt(Instant.now())
                .paymentCheckType(paymentCheckType)
                .paymentType(paymentType)
                .build();
        return paymentHistoryRepository.save(paymentHistory);
    }

    private IamportResponse<com.siot.IamportRestClient.response.Payment> getIamportResponse(
            PaymentCallbackRequest request)
            throws IamportResponseException, IOException {

        IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                iamportClient.paymentByImpUid(request.getPaymentUid());
        return iamportResponse;
    }
}
