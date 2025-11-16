package org.fpj.users.domain;

import java.time.Instant;

public record ConversationMessageView(
        Long id,
        Instant createdAt,
        String content,
        String senderUsername,
        String recipientUsername,
        String messageType // "DIRECT" oder "PINBOARD"
) {}

