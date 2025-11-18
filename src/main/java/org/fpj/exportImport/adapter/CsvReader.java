package org.fpj.exportImport.adapter;

import java.io.InputStream;

public interface CsvReader {
    public CsvImportResult parse(InputStream in) throws Exception;
}
