package org.fpj.payments.application;

public record TransactionRow(
        Long id,
        java.math.BigDecimal amount,
        java.time.Instant createdAt,
        org.fpj.Data.TransactionType type,
        Long senderId,
        String senderUsername,
        Long recipientId,
        String recipientUsername,
        String description
) {}