package org.fpj.messaging.domain;

import org.fpj.messaging.domain.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DirectMessageRepositoryCustom {
    Page<DirectMessage> findConversation(long userA, long userB, Pageable pageable);
}
