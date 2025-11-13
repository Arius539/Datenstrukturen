package org.fpj.payments.domain;

public record TransactionRow(
        Long id,
        java.math.BigDecimal amount,
        java.time.Instant createdAt,
        TransactionType type,
        Long senderId,
        String senderUsername,
        Long recipientId,
        String recipientUsername,
        String description
) {}