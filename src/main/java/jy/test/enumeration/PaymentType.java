package jy.test.enumeration;

import java.util.Optional;

public enum PaymentType {
    REGULAR,
    IRREGULAR,
    ;

    public static Optional<PaymentType> of(String upperCasePaymentType) {
        for (PaymentType type : PaymentType.values()) {
            if (type.toString().equals(upperCasePaymentType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
