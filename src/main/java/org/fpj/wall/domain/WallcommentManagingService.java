package org.fpj.wall.domain;

import org.fpj.Exceptions.DataNotPresentException;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WallcommentManagingService {

    private final WallCommentRepository wallCommentRepo;

    @Autowired
    public WallcommentManagingService(WallCommentRepository wallCommentRepo){
        this.wallCommentRepo = wallCommentRepo;
    }

    public List<WallComment> getWallcommentsForOwner(User owner){
        Optional<List<WallComment>> wallComments = wallCommentRepo.getWallCommentsByWallOwner(owner);
        if (wallComments.isPresent()){
            return wallComments.get();
        }
        else {
            throw new DataNotPresentException("Keine WallComments für Owner " + owner.getUsername() +
                    " vorhanden.");
        }
    }

    public WallComment saveWallComment(final WallComment wallComment){
        wallComment.setCreatedAt(LocalDateTime.now());
        WallComment saved = wallCommentRepo.save(wallComment);
        return saved;
    }

}


