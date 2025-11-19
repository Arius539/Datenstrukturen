package org.fpj.exportImport.application;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.Getter;
import org.fpj.Data.UiHelpers;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.wall.domain.WallComment;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

@Getter
public class WallCommentCsvExporter {
    boolean isRunning = false;

    public void export(Iterator<WallComment> messages, OutputStream out) {
        isRunning = true;
        CsvWriterSettings settings = new CsvWriterSettings();
        settings.getFormat().setDelimiter(';');
        settings.setHeaders("Zeitpunkt der Nachricht", "Sender", "Empfänger", "Nachricht");
        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            CsvWriter writer = new CsvWriter(osw, settings);

            writer.writeHeaders();
            Iterable<WallComment> iterable = () -> messages;
            for (WallComment n : iterable) {
                String description = UiHelpers.safe(n.getContent());
                writer.writeRow(
                        formatInstant(n.getCreatedAt()),
                        n.getAuthor().getUsername(),
                        n.getWallOwner().getUsername(),
                        UiHelpers.truncateFull(description, description.length()));
            }
            writer.flush();
            isRunning = false;
        } catch (Exception e) {
            isRunning = false;
            throw new RuntimeException("Fehler beim Export der Pinnwandkommentare als CSV", e);
        }
    }

    private String formatInstant(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Europe/Berlin"));
        return formatter.format(instant);
    }
}
