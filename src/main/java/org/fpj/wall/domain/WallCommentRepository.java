package org.fpj.wall.domain;


import org.fpj.users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface WallCommentRepository extends JpaRepository<WallComment, Long> {
    Optional<List<WallComment>> getWallCommentsByWallOwner(User wallOwner);
    Optional<List<WallComment>> getWallCommentsByAuthor(User author);
}


