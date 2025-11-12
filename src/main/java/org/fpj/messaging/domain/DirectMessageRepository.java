package org.fpj.messaging.domain;

import org.fpj.messaging.domain.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long>{
    Page<DirectMessage> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Page<DirectMessage> findBySender_IdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
}
