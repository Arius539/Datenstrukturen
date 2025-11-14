package org.fpj.messaging.application;
import java.time.LocalDateTime;

public record ChatPreview(String name, String lastMessage, LocalDateTime timestamp, String lastMessageUsername) {}