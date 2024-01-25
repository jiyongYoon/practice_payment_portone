package jy.test.repository;

import jy.test.entity.Order;

import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findOrderFetchPaymentAndMember(String orderUid);

    Optional<Order> findOrderFetchPayment(String orderUid);

    Order save(Order order);

    void delete(Order order);
}
