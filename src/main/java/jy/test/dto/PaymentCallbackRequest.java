package jy.test.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jy.test.enumeration.PaymentType;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ToString
public class PaymentCallbackRequest {
    private String paymentUid; // 결제 고유 번호 (imp_uid)
    private String orderUid; // 주문 고유 번호 (merchant_uid)
    private String customerUid; // 빌링키 1:1 매핑키 (customer_uid)
    private String status; // 결제 상태 (status)
    private PaymentType paymentType; // 정기 / 비정기

    public static PaymentDto toPaymentDto(PaymentCallbackRequest request) {
        return PaymentDto.builder()
                .orderUid(request.getOrderUid())
                .paymentUid(request.getPaymentUid())
                .customerUid(request.getCustomerUid())
                .build();
    }
}
