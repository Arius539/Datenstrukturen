package org.fpj.users.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<UsernameOnly> findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String term);

    @Query("""
        select u
        from User u
        where u.id <> :a
          and exists (
                select 1
                from DirectMessage dm
                where (dm.sender.id = :a and dm.recipient.id = u.id)
                   or (dm.sender.id = u.id and dm.recipient.id = :a)
          )
        order by u.username asc
    """)
    Page<User> findContacts(@Param("a") Long userId, Pageable pageable);

    @Query(
            value = """
            select u.*
            from users u
            join (
                select contact_id, max(created_at) as last_at
                from (
                    select dm.recipient as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.sender = :a
                    union all
                    select dm.sender   as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.recipient = :a
                ) x
                group by contact_id
            ) c on c.contact_id = u.id
            order by c.last_at desc
            """,
            countQuery = """
            select count(*) 
            from (
                select contact_id
                from (
                    select dm.recipient as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.sender = :a
                    union all
                    select dm.sender   as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.recipient = :a
                ) x
                group by contact_id
            ) contacts
            """,
            nativeQuery = true
    )
    Page<User> findContactsOrderByLastMessageDesc(@Param("a") Long userId, Pageable pageable);

    @Query(
            value = """
            SELECT 
                m.id                 AS id,
                m.created_at         AS createdAt,
                m.content            AS content,
                m.senderUsername     AS senderUsername,
                m.recipientUsername  AS recipientUsername,
                m.message_type       AS messageType
            FROM (
                -- Direct Messages
                SELECT
                    dm.id,
                    dm.created_at,
                    dm.content,
                    s.username AS senderUsername,
                    r.username AS recipientUsername,
                    'DIRECT'   AS message_type
                FROM direct_messages dm
                JOIN users s ON s.id = dm.sender
                JOIN users r ON r.id = dm.recipient
                WHERE (dm.sender = :userId1 AND dm.recipient = :userId2)
                   OR (dm.sender = :userId2 AND dm.recipient = :userId1)

                UNION ALL

                -- Pinboard Kommentare
                SELECT
                    pc.id,
                    pc.created_at,
                    pc.content,
                    a.username AS senderUsername,
                    o.username AS recipientUsername,
                    'PINBOARD' AS message_type
                FROM pinboard_comments pc
                JOIN users a ON a.id = pc.author_id
                JOIN users o ON o.id = pc.wall_owner_id
                WHERE (pc.author_id = :userId1 AND pc.wall_owner_id = :userId2)
                   OR (pc.author_id = :userId2 AND pc.wall_owner_id = :userId1)
            ) AS m
            ORDER BY m.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT dm.id
                FROM direct_messages dm
                WHERE (dm.sender = :userId1 AND dm.recipient = :userId2)
                   OR (dm.sender = :userId2 AND dm.recipient = :userId1)

                UNION ALL

                SELECT pc.id
                FROM pinboard_comments pc
                WHERE (pc.author_id = :userId1 AND pc.wall_owner_id = :userId2)
                   OR (pc.author_id = :userId2 AND pc.wall_owner_id = :userId1)
            ) AS m
            """,
            nativeQuery = true
    )
    List<ConversationMessageView> findConversationBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

}

