package org.fpj.payments.domain;
import org.fpj.payments.application.TransactionRow;
import org.fpj.payments.application.TransactionRow;
import org.fpj.Data.TransactionType;
import org.fpj.payments.application.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySender_IdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    Page<Transaction> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Page<Transaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    @Query(
            value = """
        select new org.fpj.payments.application.TransactionRow(
            t.id, t.amount, t.createdAt, t.transactionType,
            s.id, s.username, r.id, r.username, t.description
        )
        from Transaction t
        left join t.sender s
        left join t.recipient r
        where (s.id = :partyId or r.id = :partyId)
        order by t.createdAt desc
    """,
            countQuery = """
        select count(t)
        from Transaction t
        left join t.sender s
        left join t.recipient r
        where (s.id = :partyId or r.id = :partyId)
    """
    )
    Page<TransactionRow> findRowsForUser(@Param("partyId") long partyId, Pageable pageable);


    @Query(value = """
        SELECT
          COALESCE(SUM(CASE WHEN t.recipient = :userId THEN t.amount ELSE 0 END), 0)
          - COALESCE(SUM(CASE WHEN t.sender    = :userId THEN t.amount ELSE 0 END), 0)
        FROM transactions t
        """,
            nativeQuery = true
    )
    BigDecimal computeBalance(@Param("userId") long userId);
}
