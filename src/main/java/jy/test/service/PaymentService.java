package jy.test.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import jy.test.dto.PaymentCallbackRequest;
import jy.test.dto.RequestPayDto;
import jy.test.entity.Order;
import jy.test.entity.Payment;
import jy.test.entity.PaymentHistory;
import jy.test.enumeration.PaymentCheckType;
import jy.test.enumeration.PaymentStatus;
import jy.test.persistence.impl.OrderRepositoryImpl;
import jy.test.persistence.impl.PaymentHistoryRepositoryImpl;
import jy.test.persistence.impl.PaymentRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final OrderRepositoryImpl orderRepository;
    private final PaymentRepositoryImpl paymentRepository;
    private final PaymentHistoryRepositoryImpl paymentHistoryRepository;
    private final IamportClient iamportClient;

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
                .build();
    }

    /**
     * 콜백 요청을 통한 결제 검증
     */
    public IamportResponse<com.siot.IamportRestClient.response.Payment> paymentByCallback(PaymentCallbackRequest request) {
        savePaymentHistory(request, PaymentCheckType.CALLBACK);

        try {
            // 결제 단건 조회
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    getIamportResponse(request);

            Order order = orderRepository.findOrderFetchPayment(request.getOrderUid())
                    .orElseThrow(NoSuchElementException::new);

            validatePaymentAndCancelIfAmountDifferent(iamportResponse, order);

            Payment payment = order.getPayment();
            if (!PaymentStatus.OK.equals(payment.getStatus())) {
                payment.changePaymentBySuccess(
                        PaymentStatus.OK, iamportResponse.getResponse().getImpUid(), PaymentCheckType.CALLBACK);
                log.info("{}으로 결제 완료!, payment_uid={}, order_uid={}",
                        PaymentCheckType.CALLBACK, payment.getPaymentUid(), order.getOrderUid());
            }

            return iamportResponse;

        } catch (IamportResponseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 웹훅 요청을 통한 결제 검증
     */
    public IamportResponse<com.siot.IamportRestClient.response.Payment> paymentByWebhook(PaymentCallbackRequest request) {
        savePaymentHistory(request, PaymentCheckType.WEBHOOK);

        try {
            // 결제 단건 조회
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    getIamportResponse(request);

            Order order = orderRepository.findOrderFetchPayment(request.getOrderUid())
                    .orElseThrow(NoSuchElementException::new);

            validatePaymentAndCancelIfAmountDifferent(iamportResponse, order);

            Payment payment = order.getPayment();
            if (!PaymentStatus.OK.equals(payment.getStatus())) {
                payment.changePaymentBySuccess(
                        PaymentStatus.OK, iamportResponse.getResponse().getImpUid(), PaymentCheckType.WEBHOOK);
                log.info("{}으로 결제 완료!, payment_uid={}, order_uid={}",
                        PaymentCheckType.WEBHOOK, payment.getPaymentUid(), order.getOrderUid());
            }
            return iamportResponse;

        } catch (IamportResponseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 포트원으로부터 받아온 결제 데이터를 가지고 검증하는 메서드. <br>
     * 결제 완료가 되지 않음 -> 주문정보 및 결제정보 삭제 <br>
     * 결제 완료는 되었으나 결제 금액이 다름 -> 주문정보 및 결제정보 삭제 후, 포트원에 결제 취소 요청
     * @param iamportResponse 포트원으로부터 받은 결제 데이터
     * @param order 고객의 주문 정보
     */
    private void validatePaymentAndCancelIfAmountDifferent(IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse,
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

    private void savePaymentHistory(PaymentCallbackRequest request, PaymentCheckType type) {
        PaymentHistory paymentHistory = PaymentHistory.builder()
                .paymentUid(request.getPaymentUid())
                .orderUid(request.getOrderUid())
                .paymentCheckType(type)
                .createdAt(Instant.now())
                .build();
        paymentHistoryRepository.save(paymentHistory);
    }

    private IamportResponse<com.siot.IamportRestClient.response.Payment> getIamportResponse(PaymentCallbackRequest request) throws IamportResponseException, IOException {
        IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                iamportClient.paymentByImpUid(request.getPaymentUid());
        return iamportResponse;
    }

}
