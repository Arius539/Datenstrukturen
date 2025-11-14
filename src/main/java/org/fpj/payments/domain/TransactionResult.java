package org.fpj.payments.domain;

import java.math.BigDecimal;

public record TransactionResult(
        Transaction transaction,
        BigDecimal newBalance
) {}
