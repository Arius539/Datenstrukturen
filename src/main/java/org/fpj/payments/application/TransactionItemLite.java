package org.fpj.payments.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionItemLite(String counterparty, BigDecimal amount, LocalDateTime timestamp, String subject) {}