package org.fpj.messaging.domain;

import org.fpj.messaging.domain.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long>{
    Page<DirectMessage> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Page<DirectMessage> findBySender_IdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    @Query("""
        select dm
        from DirectMessage dm
        where (dm.sender.id = :a and dm.recipient.id = :b)
           or (dm.sender.id = :b and dm.recipient.id = :a)
        order by dm.createdAt desc
    """)
    Page<DirectMessage> findConversation(@Param("a") long userA,
                                         @Param("b") long userB,
                                         Pageable pageable);
}
