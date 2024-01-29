package jy.test.entity;

import jy.test.enumeration.PaymentCheckType;
import jy.test.enumeration.PaymentHistoryType;
import jy.test.enumeration.PaymentType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String paymentUid; // 결제 고유번호
    private String orderUid; // 주문번호
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    private PaymentCheckType paymentCheckType; // 콜백 / 웹훅
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType; // 정기 / 비정기

    @Enumerated(EnumType.STRING)
    private PaymentHistoryType paymentHistoryType;

    public void markHistoryType(PaymentHistoryType paymentHistoryType) {
        this.paymentHistoryType = paymentHistoryType;
    }

    @Builder
    public PaymentHistory(String paymentUid, String orderUid, Instant createdAt,
                          PaymentCheckType paymentCheckType, PaymentType paymentType, PaymentHistoryType paymentHistoryType) {
        this.paymentUid = paymentUid;
        this.orderUid = orderUid;
        this.createdAt = createdAt;
        this.paymentCheckType = paymentCheckType;
        this.paymentType = paymentType;
        this.paymentHistoryType = paymentHistoryType;
    }
}
