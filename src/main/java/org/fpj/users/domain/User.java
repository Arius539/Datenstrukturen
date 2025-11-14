package org.fpj.users.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fpj.wall.domain.WallComment;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(
        name = "users",
        indexes = {
                @Index(name = "users_username_uq", columnList = "username", unique = true)
        }
)
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 320)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /*
    sollten wir doch lieber dynamisch laden, immer 100 am Stück, erst wenn der Nutzer es will oder?
    @OneToMany(mappedBy = "wallComment", cascade = CascadeType.ALL)
    private List<WallComment> wallComments;*/

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (username != null) username = username.toLowerCase();
    }

}
