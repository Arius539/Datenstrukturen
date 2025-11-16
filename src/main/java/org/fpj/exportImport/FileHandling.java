package org.fpj.exportImport;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
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

    /**
     * Liefert einen InputStream für die zuvor ausgewählte Datei.
     *
     * @throws IllegalStateException    wenn noch keine Datei ausgewählt wurde
     * @throws FileNotFoundException    wenn die Datei nicht (mehr) existiert
     */
    public static InputStream getSelectedFileInputStream(File selectedFile) throws FileNotFoundException {
        if (selectedFile == null) {
            throw new IllegalStateException("Es wurde noch keine Datei über den File-Chooser ausgewählt.");
        }
        return new FileInputStream(selectedFile);
    }

    public static InputStream openFileAsStream(String filePath) throws IOException {
        return Files.newInputStream(Path.of(filePath));
    }
}
