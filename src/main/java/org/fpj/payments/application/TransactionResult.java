package org.fpj.payments.application;

import org.fpj.payments.domain.Transaction;

import java.math.BigDecimal;

public record TransactionResult(
        Transaction transaction,
        BigDecimal newBalance,
        TransactionItemLite itemLite
) {}
