package jy.test.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ToString
public class PaymentCallbackRequest {
    private String paymentUid; // 결제 고유 번호
    private String orderUid; // 주문 고유 번호
    private String status; // 결제 상태
}
