package org.fpj.payments.domain;

import org.fpj.Data.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySender_IdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    Page<Transaction> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Page<Transaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    @Query("""
        select t
        from Transaction t
        where (t.sender = :partyId or t.recipient = :partyId)
        order by t.createdAt desc
    """)
    Page<Transaction> findByTransactionPartyOrderByCreatedAtDesc(@Param("partyId") long partyId, Pageable pageable);

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
