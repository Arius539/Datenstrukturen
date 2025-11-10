package org.fpj.users.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fpj.wall.domain.WallComment;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private String password;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "wallComment", cascade = CascadeType.ALL)
    private List<WallComment> wallComments;


}
