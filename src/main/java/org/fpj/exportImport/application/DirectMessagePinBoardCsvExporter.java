package org.fpj.exportImport.application;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.Getter;
import org.fpj.Data.UiHelpers;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.users.domain.ConversationMessageView;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
@Getter
public class DirectMessagePinBoardCsvExporter {
    boolean isRunning = false;

    public void export(Iterator<ConversationMessageView> messages, OutputStream out) {
        isRunning = true;
        CsvWriterSettings settings = new CsvWriterSettings();
        settings.getFormat().setDelimiter(';');
        settings.setHeaders("Zeitpunkt der Nachricht", "Sender", "Empfänger", "Nachricht");
        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            CsvWriter writer = new CsvWriter(osw, settings);

            writer.writeHeaders();
            Iterable<ConversationMessageView> iterable = () -> messages;
            for (ConversationMessageView n : iterable) {
                writer.writeRow(
                        formatInstant(n.createdAt()),
                        n.senderUsername(),
                        n.recipientUsername(),
                        n.content());
            }
            writer.flush();
            isRunning = false;
        } catch (Exception e) {
            isRunning = false;
            throw new RuntimeException("Fehler beim Export der Direktnachrichten als CSV", e);
        }
    }

    private String formatInstant(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Europe/Berlin"));
        return formatter.format(instant);
    }
}
