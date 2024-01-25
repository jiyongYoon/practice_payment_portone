package jy.test.repository;

import jy.test.entity.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);

    void delete(Payment payment);
}
