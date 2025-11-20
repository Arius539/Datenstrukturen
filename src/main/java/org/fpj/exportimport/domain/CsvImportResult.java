package org.fpj.exportimport.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public final class CsvImportResult<T> {

    private final List<T> records;
    private final List<CsvError> errors;
    private final boolean fatal;

    public CsvImportResult(List<T> records, List<CsvError> errors, boolean fatal) {
        this.records = List.copyOf(records);
        this.errors = List.copyOf(errors);
        this.fatal = fatal;
    }

    public boolean hasFatalError() {
        return fatal;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // Getter für records und errors
}
