package org.fpj.wall.domain;


import org.fpj.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface WallCommentRepository extends JpaRepository<WallComment, Long> {
    Page<WallComment> findByWallOwner_IdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    Page<WallComment> findByAuthor_IdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
}


