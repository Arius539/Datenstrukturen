package org.fpj.javafxController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.fpj.exportImport.CsvError;
import org.fpj.exportImport.CsvImportResult;
import org.fpj.exportImport.FileHandling;
import org.fpj.exportImport.application.CsvReader;
import org.fpj.users.application.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javafx.scene.layout.Region;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

@Component
public class CsvImportDialogController<e> {
    @FXML
    private Button chooseFileButton;

    @FXML
    private Button startImportButton;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private ListView<CsvError> errorListView;

    private ObservableList<CsvError> errorList = FXCollections.observableArrayList();

    private String selectedFilePath;

    private CsvReader csvReader;

    private Consumer<List<e>> csvImportConsumer;

    public void initialize(CsvReader csvReader, Consumer<List<e>> csvImportConsumer) {
        if(csvReader == null) throw  new IllegalStateException("csvReader is null");
        this.csvReader = csvReader;
        selectedFileLabel.setText("Keine Datei ausgewählt");
        errorListView.setItems(FXCollections.observableArrayList());
        this.csvImportConsumer = csvImportConsumer;
        initErrorList();
    }

    @FXML
    private void onChooseFile(ActionEvent event) {
        Window window = chooseFileButton.getScene().getWindow();
        String path= FileHandling.openFileChooserAndGetPath(window);
        if (path != null) {
            this.selectedFilePath = path;
            selectedFileLabel.setText(path);
        }
    }

    @FXML
    private void onStartImport(ActionEvent event) {
        errorListView.getItems().clear();

        if (selectedFilePath == null) {
            error("Bitte zuerst eine CSV-Datei auswählen.");
            return;
        }

        try {
            InputStream inputStream= FileHandling.openFileAsStream(selectedFilePath);

            CsvImportResult<e> result= csvReader.parse(inputStream);

            if (result.getErrors().isEmpty()) {
                info("Import erfolgreich ohne Fehler.");
                csvImportConsumer.accept(result.getRecords());
            } else {
                errorListView.getItems().addAll(result.getErrors());
            }

        } catch (Exception ex) {
            info("Unerwarteter Fehler: " + ex.getMessage());
        }
    }

    private void initErrorList() {
        errorListView.setItems(errorList);

        errorListView.setCellFactory(list -> new ListCell<CsvError>() {
            private final Label title = new Label();
            private final Label subtitle = new Label();
            private final Label value = new Label();
            private final VBox left = new VBox(2, title, subtitle);
            private final Region spacer = new Region();
            private final HBox root = new HBox(12, left, spacer, value);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(CsvError item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Severity als Text/Color
                    String severityText = switch (item.getSeverity()) {
                        case FATAL   -> "FATAL";
                        case ERROR   -> "ERROR";
                        case WARNING -> "WARNUNG";
                    };

                    String columnPart = (item.getColumnName() != null)
                            ? ", Spalte \"" + item.getColumnName() + "\""
                            : "";

                    title.setText(severityText + " in Zeile " + item.getLine() + columnPart);
                    title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;" + severityColor(item.getSeverity()));

                    subtitle.setText(item.getMessage());
                    subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

                    String raw = item.getRawValue();
                    value.setText(raw != null && !raw.isBlank() ? raw : "");

                    setGraphic(root);

                    // Optional: Tooltip mit allen Details
                    setTooltip(new Tooltip(
                            "Zeile: " + item.getLine() +
                                    (item.getColumnName() != null ? "\nSpalte: " + item.getColumnName() : "") +
                                    (item.getColumnIndex() != null ? "\nSpaltenindex: " + item.getColumnIndex() : "") +
                                    "\nSchweregrad: " + severityText +
                                    "\nNachricht: " + item.getMessage() +
                                    (raw != null && !raw.isBlank() ? "\nWert: " + raw : "")
                    ));

                    // Optional: Färbung je nach Severity für das gesamte Element
                    switch (item.getSeverity()) {
                        case FATAL   -> setStyle("-fx-background-color: #ffeaea;");
                        case ERROR   -> setStyle("-fx-background-color: #fff3cd;");
                        case WARNING -> setStyle("-fx-background-color: #e7f7fd;");
                    }
                }
            }

            // Helper für farbliche Markierung des Titels nach Severity
            private String severityColor(CsvError.Severity severity) {
                return switch (severity) {
                    case FATAL   -> "-fx-text-fill: #c0392b;";
                    case ERROR   -> "-fx-text-fill: #b8860b;";
                    case WARNING -> "-fx-text-fill: #2980b9;";
                };
            }
        });
    }


    private void info(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    private void error(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        a.setHeaderText("Fehler");
        a.setTitle("Fehler");
        a.showAndWait();
    }
}
