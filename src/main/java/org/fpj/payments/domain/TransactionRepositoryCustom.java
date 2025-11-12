package org.fpj.payments.domain;

import java.math.BigDecimal;

public interface TransactionRepositoryCustom {
    BigDecimal computeBalance(long userId);
}
