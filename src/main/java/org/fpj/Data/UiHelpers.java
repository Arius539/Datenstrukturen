package org.fpj.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class UiHelpers {
    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public static String formatEuro(BigDecimal amt) {
        BigDecimal v = (amt != null ? amt : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return EUR.format(v);
    }

    public static String formatSignedEuro(BigDecimal amt) {
        BigDecimal v = (amt != null ? amt : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String s = EUR.format(v.abs());
        return (v.signum() < 0 ? "-" : "+") + " " + s;
    }

    public static BigDecimal parseAmountTolerant(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Betrag ist erforderlich.");
        }

        String s = raw.replace("€","").replaceAll("[\\s\\u00A0]", "").trim();
        int lastComma = s.lastIndexOf(',');
        int lastDot   = s.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.');
        }

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Betrag konnte nicht gelesen werden.");
        }
    }

    public static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
