package jy.test.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jy.test.enumeration.PaymentType;
import lombok.*;

import java.time.Instant;

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
    private Instant paidAt; // 결제일시

    public static PaymentDto toPaymentDto(PaymentCallbackRequest request) {
        return PaymentDto.builder()
                .orderUid(request.getOrderUid())
                .paymentUid(request.getPaymentUid())
                .customerUid(request.getCustomerUid())
                .build();
    }

    public static PaymentCallbackRequest fromString(String request) {
        JsonObject requestJson = JsonParser.parseString(request).getAsJsonObject();
        String paymentUid = requestJson.getAsJsonPrimitive("imp_uid") == null ? null : requestJson.getAsJsonPrimitive("imp_uid").getAsString();
        String orderUid = requestJson.getAsJsonPrimitive("merchant_uid") == null ? null : requestJson.getAsJsonPrimitive("merchant_uid").getAsString();
        String customerUid = requestJson.getAsJsonPrimitive("customer_uid") == null ? null : requestJson.getAsJsonPrimitive("customer_uid").getAsString();
        String status = requestJson.getAsJsonPrimitive("status") == null ? null : requestJson.getAsJsonPrimitive("status").getAsString();
        PaymentType paymentType = requestJson.getAsJsonPrimitive("pay_type") == null ? null : PaymentType.of(requestJson.getAsJsonPrimitive("pay_type").getAsString()).get();
        Instant paidAt = requestJson.getAsJsonPrimitive("paid_at") == null ? null : Instant.ofEpochSecond(requestJson.getAsJsonPrimitive("paid_at").getAsLong());

        return PaymentCallbackRequest.builder()
                .paymentUid(paymentUid)
                .orderUid(orderUid)
                .customerUid(customerUid)
                .paymentType(paymentType)
                .status(status)
                .paidAt(paidAt)
                .build();
    }
}
