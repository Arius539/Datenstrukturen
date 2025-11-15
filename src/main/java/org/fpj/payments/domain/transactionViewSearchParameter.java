package org.fpj.payments.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record transactionViewSearchParameter(Long currentUserID, String description, Instant createdFrom, Instant createdTo, String senderUsername, String recipientUsername, BigDecimal amountFrom, BigDecimal amountTo)  {

}
