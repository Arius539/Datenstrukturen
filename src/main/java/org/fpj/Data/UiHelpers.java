package org.fpj.Data;

import javafx.concurrent.Task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

public class UiHelpers {
    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    private static final int MAX_LENGTH_EMAIL = 320;


    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public static void isValidEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("E-Mail bzw. der Benutzername darf nicht null sein.");
        }

        if (email.isBlank()) {
            throw new IllegalArgumentException("E-Mail bzw. der Benutzername darf nicht leer sein.");
        }

        if (email.length() > MAX_LENGTH_EMAIL) {
            throw new IllegalArgumentException(
                    "E-Mail bzw. der Benutzername überschreitet die maximale Länge von " + MAX_LENGTH_EMAIL + " Zeichen."
            );
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("E-Mail bzw. der Benutzername hat kein gültiges Format (z.B. firstname.lastname@domain.de).");
        }
    }

    public static String truncate(String s, int max) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] lines = s.split("\\R");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(System.lineSeparator());
                sb.append(line);
            }
        }
        String result = sb.toString();
        return result.length() <= max ? result : result.substring(0, max) + "…";
    }
    /**Punkt als Decimal Trennzeichen, kein Währungszeichen und kein Vorzeichen*/
    public static String formatBigDecimal(BigDecimal amt) {
        BigDecimal v = (amt != null ? amt : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return v.toPlainString();
    }


    /**Komma als Decimal Trennzeichen und kein Währungszeichen und kein Vorzeichen*/
    public static String formatEuro(BigDecimal amt) {
        BigDecimal v = (amt != null ? amt : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return EUR.format(v);
    }

    /**Komma als Decimal Trennzeichen und Währungszeichen und Vorzeichen*/
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

    public static LocalDate parseDateTolerant(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Eingabefeld darf nicht leer sein");
        }
        text = text.trim();

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE,                // "yyyy-MM-dd"
                DateTimeFormatter.ofPattern("dd.MM.yy"),
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("d.M.yy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException e) {
            }
        }
        throw new IllegalArgumentException("Bitte ein gültiges Datum eingeben, z.B. 16.11.2025 oder 2025-11-16");
    }

    public static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public static String formatInstant(Instant t) {
        if (t == null) {
            return "";
        }

        DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.of("Europe/Berlin"));

        return fmt.format(t);
    }

    public static String formatInstantToDate(Instant instant) {
        DateTimeFormatter dateFormatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("Europe/Berlin"));

        return dateFormatter.format(instant);
    }

    public static void startBackgroundTask(Task<?> task, String threadName) {
        Thread t = new Thread(task, threadName);
        t.setDaemon(true);
        t.start();
    }
}
