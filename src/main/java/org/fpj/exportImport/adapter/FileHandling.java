package org.fpj.exportImport.adapter;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHandling {
    public static String openFileChooserAndGetPath(Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Datei auswählen");
        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile != null) {
            return selectedFile.getAbsolutePath();
        } else {
            return null;
        }
    }

    public static OutputStream openFileAsOutStream(String filePath) throws IOException {
        return Files.newOutputStream(Path.of(filePath));
    }

    public static InputStream openFileAsStream(String filePath) throws IOException {
        return Files.newInputStream(Path.of(filePath));
    }
}
