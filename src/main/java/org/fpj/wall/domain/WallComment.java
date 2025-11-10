package org.fpj.wall.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fpj.users.domain.User;

import java.sql.Timestamp;

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
    @OneToOne
    @JoinColumn(name = "users")
    private User wallOwner;
    @OneToOne
    @JoinColumn(name = "users")
    private User author;
    private Timestamp createdAt;

}
