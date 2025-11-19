package org.fpj.exportImport.application;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.Getter;
import org.fpj.Data.UiHelpers;
import org.fpj.payments.domain.TransactionRow;
import org.fpj.users.application.UserService;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
@Getter
public class TransactionCsvExporter {
    boolean isRunning = false;

    public void export(Iterator<TransactionRow> transactions, OutputStream out) {
        this.isRunning = true;
        CsvWriterSettings settings = new CsvWriterSettings();
        settings.getFormat().setDelimiter(';');
        settings.setHeaders("Transaktionsdatum", "Empfänger", "Sender", "Beschreibung", "Betrag", "Transaktionstyp");

        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            CsvWriter writer = new CsvWriter(osw, settings);

            writer.writeHeaders();
            Iterable<TransactionRow> iterable = () -> transactions;
            for (TransactionRow t : iterable) {
                String description =  UiHelpers.safe(t.description());
                writer.writeRow(
                        UiHelpers.formatInstantToDate(t.createdAt()),
                        t.recipientUsername(),
                        t.senderUsername(),
                        UiHelpers.truncateFull(description, description.length()),
                        UiHelpers.formatBigDecimal(t.amount().abs()),
                        t.type().name()
                );
            }
            writer.flush();
            this.isRunning = false;
        } catch (Exception e) {
            this.isRunning = false;
            throw new RuntimeException("Fehler beim Export der Transaktionen als CSV", e);
        }
    }
}



