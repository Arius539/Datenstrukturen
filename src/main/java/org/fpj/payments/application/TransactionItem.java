package org.fpj.payments.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionItem(String Sender, String Recipient, BigDecimal amount, LocalDateTime timestamp, String subject, String type) {
}