package org.fpj.wall.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fpj.users.domain.User;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class WallComment {

    @Id
    @GeneratedValue
    private Long id;
    private String content;
    @ManyToOne
    @JoinColumn(name = "users")
    private User wallOwner;
    @ManyToOne
    @JoinColumn(name = "users")
    private User author;
    private LocalDateTime createdAt;

}
