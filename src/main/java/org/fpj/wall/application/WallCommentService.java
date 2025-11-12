package org.fpj.wall.application;

import org.fpj.users.domain.User;
import org.fpj.wall.domain.WallComment;
import org.fpj.wall.domain.WallcommentManagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class WallCommentService {

    private final WallcommentManagingService wallcommentManagingService;
    private final ApplicationContext context;

    @Autowired
    public WallCommentService(WallcommentManagingService wallcommentManagingService,
                              ApplicationContext context){
        this.wallcommentManagingService = wallcommentManagingService;
        this.context = context;
    }

    public Page<WallComment> seeMyPinwall(){
        final User owner = (User) context.getBean("loggedInUser");
        return seePinwall(owner);
    }

    public Page<WallComment> seePinwall(final User pinwallOwner){
        return wallcommentManagingService.getWallcommentsForOwner(pinwallOwner);
    }
}
