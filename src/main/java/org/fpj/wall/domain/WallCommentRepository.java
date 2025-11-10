package org.fpj.wall.domain;


import org.fpj.users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface WallCommentRepository extends JpaRepository<WallComment, Long> {
    List<WallComment> getWallcommentsForOwner(User owner);
}


