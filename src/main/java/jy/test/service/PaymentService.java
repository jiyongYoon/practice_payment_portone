package jy.test.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import jy.test.dto.PaymentCallbackRequest;
import jy.test.dto.RequestPayDto;
import jy.test.entity.Order;
import jy.test.entity.Payment;
import jy.test.enumeration.PaymentStatus;
import jy.test.persistence.impl.OrderRepositoryImpl;
import jy.test.persistence.impl.PaymentRepositoryImpl;
import jy.test.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepositoryImpl orderRepository;
    private final PaymentRepositoryImpl paymentRepository;
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

    public IamportResponse<com.siot.IamportRestClient.response.Payment> paymentByCallback(PaymentCallbackRequest request) {
        try {
            // 결제 단건 조회
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    iamportClient.paymentByImpUid(request.getPaymentUid());

            Order order = orderRepository.findOrderFetchPayment(request.getOrderUid())
                    .orElseThrow(NoSuchElementException::new);

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

                CancelData cancelData = new CancelData(iamportResponse.getResponse().getImpUid(), true, new BigDecimal(iamportPrice));

                iamportClient.cancelPaymentByImpUid(cancelData);

                throw new RuntimeException("결제금액 상이하여 취소");
            }

            order.getPayment().changePaymentBySuccess(PaymentStatus.OK, iamportResponse.getResponse().getImpUid());

            return iamportResponse;

        } catch (IamportResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
