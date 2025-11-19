package org.fpj.exportImport.domain;

import java.io.InputStream;

public interface CsvReader {
    public CsvImportResult parse(InputStream in) throws Exception;
}
