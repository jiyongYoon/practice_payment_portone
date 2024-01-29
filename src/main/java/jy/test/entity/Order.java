package jy.test.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long price;
    private String itemName;
    private String orderUid; // 주문번호
    private Instant orderedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Builder
    public Order(Long price, String itemName, String orderUid, Member member, Payment payment, Instant orderedAt) {
        this.price = price;
        this.itemName = itemName;
        this.orderUid = orderUid;
        this.member = member;
        this.payment = payment;
        this.orderedAt = orderedAt;
    }
}
