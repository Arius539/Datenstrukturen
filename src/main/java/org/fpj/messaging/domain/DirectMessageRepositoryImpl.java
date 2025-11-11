package org.fpj.messaging.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.messaging.domain.DirectMessageRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class DirectMessageRepositoryImpl implements DirectMessageRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<DirectMessage> findConversation(long userA, long userB, Pageable pageable) {
        String jpql = """
            SELECT dm FROM DirectMessage dm
             WHERE (dm.sender.id = :a AND dm.recipient.id = :b)
                OR (dm.sender.id = :b AND dm.recipient.id = :a)
             ORDER BY dm.createdAt DESC
            """;
        List<DirectMessage> items = em.createQuery(jpql, DirectMessage.class)
                .setParameter("a", userA)
                .setParameter("b", userB)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = em.createQuery("""
            SELECT COUNT(dm) FROM DirectMessage dm
             WHERE (dm.sender.id = :a AND dm.recipient.id = :b)
                OR (dm.sender.id = :b AND dm.recipient.id = :a)
            """, Long.class)
                .setParameter("a", userA)
                .setParameter("b", userB)
                .getSingleResult();

        return new PageImpl<>(items, pageable, total);
    }
}
