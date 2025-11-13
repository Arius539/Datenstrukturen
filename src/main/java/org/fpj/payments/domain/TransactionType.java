package org.fpj.payments.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

public enum TransactionType  {
    EINZAHLUNG, AUSZAHLUNG, UEBERWEISUNG;
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // wird auf Entity-Feld angewendet
    public @interface PgEnum {}
}
