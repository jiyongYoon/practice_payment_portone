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
public class PaymentDto {
    private String paymentUid; // 결제 고유 번호
    private String orderUid; // 주문 고유 번호
    private String customerUid; // 빌링키 1:1 매핑키
}
