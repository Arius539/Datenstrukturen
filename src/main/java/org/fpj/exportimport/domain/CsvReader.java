package org.fpj.exportimport.domain;

import java.io.InputStream;

public interface CsvReader {
    public CsvImportResult parse(InputStream in) throws Exception;
    public boolean isRunning();
}
