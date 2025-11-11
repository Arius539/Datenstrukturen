package org.fpj.wall.application;

import org.fpj.wall.domain.WallCommentRepository;
import org.springframework.stereotype.Service;

@Service
public class WallCommentManagingService {

    private final WallCommentRepository wallComments;

    public WallCommentManagingService(WallCommentRepository wallComments) {
        this.wallComments = wallComments;
    }
/*
    @Transactional(readOnly = true)
    public List<WallComment> getWallcommentsForOwner(User owner) {
        List<WallComment> list = wallComments.findAllByWallOwnerOrderByCreatedAtDesc(owner);
        if (list.isEmpty()) {
            throw new DataNotPresentException(
                    "Keine WallComments für Owner " + owner.getUsername() + " vorhanden."
            );
        }
        return list;
    }

    @Transactional(readOnly = true)
    public Page<WallComment> pageWallcommentsForOwner(long ownerId, Pageable pageable) {
        return wallComments.findByWallOwner_IdOrderByCreatedAtDesc(ownerId, pageable);
    }

    @Transactional
    public WallComment saveWallComment(final WallComment wallComment) {
        // createdAt NICHT hier setzen; wird in der Entity via @CreationTimestamp gesetzt.
        if (wallComment.getWallOwner() == null) {
            throw new IllegalArgumentException("wallOwner darf nicht null sein.");
        }
        if (wallComment.getContent() == null || wallComment.getContent().isBlank()) {
            throw new IllegalArgumentException("content darf nicht leer sein.");
        }
        return wallComments.save(wallComment);
    }
    */
}
