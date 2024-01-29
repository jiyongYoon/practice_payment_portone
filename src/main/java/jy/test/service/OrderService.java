package jy.test.service;

import jy.test.entity.Member;
import jy.test.entity.Order;
import jy.test.entity.Payment;
import jy.test.enumeration.PaymentStatus;
import jy.test.persistence.impl.OrderRepositoryImpl;
import jy.test.persistence.impl.PaymentRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepositoryImpl orderRepository;
    private final PaymentRepositoryImpl paymentRepository;

    public Order autoOrder(Member member, String itemName, Instant now) {
        // 임시 결제내역 생성
        Payment payment = Payment.builder()
                .price(1000L)
                .status(PaymentStatus.READY)
                .build();

        paymentRepository.save(payment);

        // 주문 생성
        Order order = Order.builder()
                .member(member)
                .price(1000L)
                .itemName(itemName)
                .orderUid(UUID.randomUUID().toString())
                .payment(payment)
                .orderedAt(now)
                .build();

        return orderRepository.save(order);
    }
}
