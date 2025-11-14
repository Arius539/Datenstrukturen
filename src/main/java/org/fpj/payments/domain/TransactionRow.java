package org.fpj.payments.domain;

import org.fpj.Data.UiHelpers;

import java.math.BigDecimal;

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
) {

    public String amountString(long currentUserId) {
        boolean outgoing = this.senderId() ==null? false : this.senderId == currentUserId;
       return UiHelpers.formatSignedEuro(!outgoing ? this.amount():new BigDecimal("0").subtract(this.amount()));
    }

    public String amountStringUnsigned() {
        return UiHelpers.formatSignedEuro( this.amount());
    }
}