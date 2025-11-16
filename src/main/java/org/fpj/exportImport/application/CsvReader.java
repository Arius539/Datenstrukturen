package org.fpj.exportImport.application;

import org.fpj.exportImport.CsvImportResult;
import org.fpj.payments.domain.MassTransfer;

import java.io.InputStream;

public interface CsvReader {
    public CsvImportResult parse(InputStream in) throws Exception;
}
