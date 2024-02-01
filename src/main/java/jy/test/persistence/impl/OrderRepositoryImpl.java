package jy.test.persistence.impl;

import jy.test.entity.Order;
import jy.test.persistence.OrderJpaRepository;
import jy.test.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Optional<Order> findOrderFetchPaymentAndMember(String orderUid) {
        return jpaRepository.findOrderFetchPaymentAndMember(orderUid);
    }

    @Override
    public Optional<Order> findOrderFetchPaymentAndMemberForUpdate(String orderUid) {
        return jpaRepository.findOrderFetchPaymentAndMemberPessimisticLock(orderUid);
    }

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public void delete(Order order) {
        jpaRepository.delete(order);
    }

}
