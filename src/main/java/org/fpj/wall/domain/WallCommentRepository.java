package org.fpj.wall.domain;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WallCommentRepository extends JpaRepository<WallComment, Long> {

    @EntityGraph(attributePaths = {"author", "wallOwner"})
    Page<WallComment> findByWallOwner_IdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "wallOwner"})
    Page<WallComment> findByAuthor_IdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "wallOwner"})
    @Query("""
           SELECT wc
           FROM WallComment wc
           WHERE wc.author.id = :authorId
           ORDER BY wc.createdAt DESC
           """)
    List<WallComment> toListByAuthor(@Param("authorId") Long authorId);

    @EntityGraph(attributePaths = {"author", "wallOwner"})
    @Query("""
           SELECT wc
           FROM WallComment wc
           WHERE wc.wallOwner.id = :ownerId
           ORDER BY wc.createdAt DESC
           """)
    List<WallComment> toListByWallOwner(@Param("ownerId") Long ownerId);
}


