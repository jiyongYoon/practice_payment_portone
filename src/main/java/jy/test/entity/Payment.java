package jy.test.entity;

import jy.test.enumeration.PaymentCheckType;
import jy.test.enumeration.PaymentStatus;
import jy.test.enumeration.PaymentType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long price;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String paymentUid; // 결제 고유 번호

    @Enumerated(EnumType.STRING)
    private PaymentCheckType paymentCheckType; // 콜백 / 웹훅

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType; // 정기 / 비정기

    private Instant paidAt; // 실제 결제 시간

    @Builder
    public Payment(Long price, PaymentStatus status) {
        this.price = price;
        this.status = status;
    }

    public void changePaymentBySuccess(String paymentUid,
                                       PaymentCheckType paymentCheckType,
                                       PaymentType paymentType,
                                       Instant paidAt) {
        this.status = PaymentStatus.OK;
        this.paymentUid = paymentUid;
        this.paymentCheckType = paymentCheckType;
        this.paymentType = paymentType;
        this.paidAt = paidAt;
    }
}
