package jy.test.repository;

import jy.test.entity.PaymentHistory;

public interface PaymentHistoryRepository {

    PaymentHistory save(PaymentHistory paymentHistory);
}
