package jy.test.persistence;

import jy.test.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o" +
            " left join fetch o.payment p" +
            " left join fetch o.member m" +
            " where o.orderUid = :orderUid")
    Optional<Order> findOrderFetchPaymentAndMember(String orderUid);

    @Query("select o from Order o" +
            " left join fetch o.payment p" +
            " left join fetch o.member m" +
            " where o.orderUid = :orderUid")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findOrderFetchPaymentAndMemberPessimisticLock(String orderUid);

}
