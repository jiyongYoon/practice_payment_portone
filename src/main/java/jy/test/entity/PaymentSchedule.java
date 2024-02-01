package jy.test.entity;

import jy.test.enumeration.SchedulingType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_schedule")
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SchedulingType schedulingType;

    private Instant willSchedulingAt; // 스케줄링 작동 시간

    private Boolean isDone; // 스케줄링 작동여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id")
    private Order order;

    @PrePersist
    public void isDonePrePersist() {
        this.isDone = false;
    }

    @Builder
    public PaymentSchedule(Long id, SchedulingType schedulingType, Instant willSchedulingAt, Order order) {
        this.id = id;
        this.schedulingType = schedulingType;
        this.willSchedulingAt = willSchedulingAt;
        this.order = order;
    }

    public void done() {
        this.isDone = true;
    }
}

