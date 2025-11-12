package org.fpj.payments.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.fpj.payments.domain.TransactionRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public BigDecimal computeBalance(long userId) {
        String sql = """
    SELECT
      COALESCE(SUM(CASE WHEN t.recipient = ?1 THEN t.amount ELSE 0 END), 0)
      - COALESCE(SUM(CASE WHEN t.sender = ?1 THEN t.amount ELSE 0 END), 0)
    FROM transactions t
        """;
        BigDecimal result = (BigDecimal) em.createNativeQuery(sql)
                .setParameter(1, userId)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }
}
