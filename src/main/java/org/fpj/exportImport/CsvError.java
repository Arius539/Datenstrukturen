package org.fpj.exportImport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class CsvError {
    public CsvError(long line, String columnName, String message, String rawValue, Severity severity,Integer columnIndex ) {
        this.line = line;
        this.columnName = columnName;
        this.message = message;
        this.rawValue = rawValue;
        this.severity = severity;
        this.columnIndex = columnIndex;

    }

    public enum Severity {
        WARNING,
        ERROR,
        FATAL
    }

    private final long line;
    private final String columnName;
    private final Integer columnIndex;
    private final String rawValue;
    private final String message;
    private final Severity severity;
}
