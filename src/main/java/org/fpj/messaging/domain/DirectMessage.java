package org.fpj.messaging.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fpj.users.domain.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Comment;
import java.time.Instant;

@Entity
@Table(
        name = "direct_messages",
        indexes = {
                @Index(name = "dm_sender_ctime", columnList = "sender,created_at"),
                @Index(name = "dm_recipient_ctime", columnList = "recipient,created_at")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender", nullable = false)      // FK-Spalte heißt "sender"
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient", nullable = false)   // FK-Spalte heißt "recipient"
    private User recipient;

    @Column(nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

