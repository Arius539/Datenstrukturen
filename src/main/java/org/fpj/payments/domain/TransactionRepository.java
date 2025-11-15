package org.fpj.payments.domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySender_IdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    Page<Transaction> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Page<Transaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    @Query(
            value = """
        select new org.fpj.payments.domain.TransactionRow(
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

    @Query(
            value = """
        WITH target AS (
            SELECT id, created_at
            FROM transactions
            WHERE id = :tId
              AND (:userId = sender OR :userId = recipient)
        ),
        user_trx AS (
            SELECT
                t.id,
                t.created_at,
                CASE
                    WHEN t.transaction_type = 'EINZAHLUNG' THEN t.amount
                    WHEN t.transaction_type = 'UEBERWEISUNG' THEN t.amount
                    ELSE 0
                END AS delta
            FROM transactions t
            JOIN target tg
              ON (
                   t.created_at < tg.created_at
                   OR (t.created_at = tg.created_at AND t.id <= tg.id)
                 )
            WHERE t.recipient = :userId

            UNION ALL
            SELECT
                t.id,
                t.created_at,
                CASE
                    WHEN t.transaction_type = 'AUSZAHLUNG' THEN -t.amount
                    WHEN t.transaction_type = 'UEBERWEISUNG' THEN -t.amount
                    ELSE 0
                END AS delta
            FROM transactions t
            JOIN target tg
              ON (
                   t.created_at < tg.created_at
                   OR (t.created_at = tg.created_at AND t.id <= tg.id)
                 )
            WHERE t.sender = :userId
        )
        SELECT SUM(delta) AS balance
        FROM user_trx
        """,
            nativeQuery = true
    )
    BigDecimal findUserBalanceAfterTransaction(@Param("userId") long userId, @Param("transactionId") long transactionId);

    @Query("""
        SELECT t
        FROM Transaction t
        LEFT JOIN t.sender s
        LEFT JOIN t.recipient r
        WHERE
            ( t.sender.id = :currentUserId
              OR
              t.recipient.id = :currentUserId
            )
            AND (:createdFrom IS NULL OR t.createdAt >= :createdFrom)

            AND (:createdTo IS NULL OR t.createdAt <= :createdTo)

            AND (:senderUsername IS NULL OR  LOWER(s.username) LIKE LOWER(CONCAT('%', :senderUsername, '%')))

            AND (:recipientUsername IS NULL OR  LOWER(r.username) LIKE LOWER(CONCAT('%', :recipientUsername, '%')))

            AND (:amountFrom IS NULL OR t.amount >= :amountFrom)

            AND (:amountTo IS NULL OR t.amount <= :amountTo)

            AND (:description IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :description, '%')))
        """)
    Page<Transaction> searchTransactions(
            @Param("currentUserId") Long currentUserId,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("senderUsername") String senderUsername,
            @Param("recipientUsername") String recipientUsername,
            @Param("amountFrom") BigDecimal amountFrom,
            @Param("amountTo") BigDecimal amountTo,
            @Param("description") String description,
            Pageable pageable
    );

}
