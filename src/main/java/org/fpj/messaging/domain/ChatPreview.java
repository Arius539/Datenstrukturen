package org.fpj.messaging.domain;
import java.time.LocalDateTime;

public record ChatPreview(String name, String lastMessage, LocalDateTime timestamp, String lastMessageUsername) {}