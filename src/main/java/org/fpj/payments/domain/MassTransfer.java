package org.fpj.payments.domain;

import java.math.BigDecimal;

public record MassTransfer(
        String empfaenger,
        BigDecimal betrag,
        String beschreibung
) {}
