package org.fpj.javafxController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fpj.Data.UiHelpers;
import org.fpj.payments.domain.TransactionRow;
import org.fpj.users.domain.User;

public class TransactionViewController {

    // --- Linke Seite: Kontostände, Filter, Transaktionsliste ---

    @FXML
    private Label currentBalanceLabel;

    @FXML
    private TextField filterTextField;

    @FXML
    private Label selectedTransactionBalanceLabel;

    @FXML
    private ComboBox<String> filterFieldComboBox;

    @FXML
    private Button clearFilterButton;

    @FXML
    private Button searchButton;

    @FXML
    private ListView<TransactionRow> transactionTable;

    private final ObservableList<TransactionRow> transactionList = FXCollections.observableArrayList();


    // --- Kontextmenü (für beide Tabellen) ---

    @FXML
    private ContextMenu transactionContextMenu;

    @FXML
    private MenuItem deleteMenuItem;          // "löschen"

    @FXML
    private MenuItem useAsTemplateMenuItem;   // "als Vorlage verwenden"

    @FXML
    private MenuItem executeMenuItem;         // "ausführen"


    // --- Rechte Seite: Formular "Transaktion ausführen" ---

    @FXML
    private TextField receiverUsernameField;

    @FXML
    private TextField amountField;

    @FXML
    private TextField purposeField;

    @FXML
    private ToggleGroup transactionTypeToggleGroup;

    @FXML
    private RadioButton depositRadio;   // "Einzahlung"

    @FXML
    private RadioButton withdrawRadio;  // "Auszahlung"

    @FXML
    private RadioButton transferRadio;  // "Überweisung"


    // --- Rechte Seite: Buttons unter dem Formular ---

    @FXML
    private Button importCsvButton;       // "von CSV"

    @FXML
    private Button executeSingleButton;   // "ausführen"

    @FXML
    private Button addToBatchButton;      // "hinzufügen"


    // --- Rechte Seite: gesammelte Transaktionen + "alle ausführen" ---

    @FXML
    private TableView<Object> batchTransactionTable; // TODO: Typ durch dein Sammel-Transaktionsmodell ersetzen

    @FXML
    private Button executeAllButton;

    private User currentUser;


    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    private void initialize(User currentUser) {
        if (transactionTable != null) {
            transactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
        if (batchTransactionTable != null) {
            batchTransactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }

        if (transactionTypeToggleGroup != null && transferRadio != null) {
            transactionTypeToggleGroup.selectToggle(transferRadio);
        }
    }

    private void initTransactionList(){
        transactionTable.setItems(transactionList);
        transactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        transactionTable.setCellFactory(list -> new ListCell<TransactionRow>() {
            private final Label title = new Label();
            private final Label subtitle = new Label();
            private final VBox left = new VBox(2, title, subtitle);
            private final Label amount = new Label();
            private final Region spacer = new Region();
            private final HBox root = new HBox(8, left, spacer, amount);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(TransactionRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean outgoing = item.senderId() == currentUser.getId();

                    String name = outgoing
                            ? (item.recipientUsername() != null ? item.recipientUsername() : "Empfänger unbekannt")
                            : (item.senderUsername() != null ? item.senderUsername() : "Sender unbekannt");

                    String counterparty = switch (item.type()) {
                        case EINZAHLUNG   -> "Einzahlung";
                        case AUSZAHLUNG   -> "Auszahlung";
                        case UEBERWEISUNG -> (outgoing ? "Überweisung an " : "Überweisung von ") + name;
                    };

                    title.setText(counterparty);
                    subtitle.setText(UiHelpers.formatInstant(item.createdAt()) + "  •  " + UiHelpers.truncate(item.description(), 20));
                    amount.setText(item.amountString(currentUser.getId()));
                    setGraphic(root);
                    int index = getIndex();
                    //ensureNextPageLoaded(index);

                    setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 2) {
                           // openTransactionDetails(item);
                        }
                    });
                }
            }
        });
    }


    // ---------------------------------------------------------------
    // Aktionen: Filter / Suche (linke Seite)
    // ---------------------------------------------------------------

    @FXML
    private void onClearFilter(ActionEvent event) {
        // TODO: Filtereingaben zurücksetzen
        // filterTextField.clear();
        // filterFieldComboBox.getSelectionModel().clearSelection();
        // Liste neu laden / Filter zurücksetzen
    }

    @FXML
    private void onSearch(ActionEvent event) {
        // TODO: Filtertext + gewähltes Feld auswerten
        // String field = filterFieldComboBox.getValue();
        // String query = filterTextField.getText();
        // Filterlogik auf transactionTable anwenden
    }


    // ---------------------------------------------------------------
    // Aktionen: Kontextmenü der Tabellen
    // (transactionTable & batchTransactionTable)
    // ---------------------------------------------------------------

    @FXML
    private void onDeleteTransaction(ActionEvent event) {
        // TODO: selektierte Einträge der aktiven Tabelle löschen
        // Unterscheide ggf. anhand event.getSource(), ob es aus der linken
        // oder rechten Tabelle kommt, falls nötig.
    }

    @FXML
    private void onUseAsTemplate(ActionEvent event) {
        // TODO: selektierte Transaktion als Vorlage ins Formular übernehmen
    }

    @FXML
    private void onExecuteSingleFromContext(ActionEvent event) {
        // TODO: selektierte Transaktion sofort ausführen
    }


    // ---------------------------------------------------------------
    // Aktionen: RadioButtons (Transaktionstyp)
    // ---------------------------------------------------------------

    @FXML
    private void onTransactionTypeChanged(ActionEvent event) {
        // TODO: Falls du abhängig vom Typ Felder änderst/aktivierst/deaktivierst
        // RadioButton source = (RadioButton) event.getSource();
        // String selected = source.getText();
    }


    // ---------------------------------------------------------------
    // Aktionen: Buttons rechts
    // ---------------------------------------------------------------

    @FXML
    private void onImportCsv(ActionEvent event) {
        // TODO: CSV-Datei auswählen, Transaktionen einlesen
        // und in batchTransactionTable einfügen
    }

    @FXML
    private void onExecuteSingle(ActionEvent event) {
        // TODO: Formularwerte validieren und einzelne Transaktion sofort ausführen
    }

    @FXML
    private void onAddToBatch(ActionEvent event) {
        // TODO: Formularwerte validieren und als geplante Transaktion
        // in batchTransactionTable hinzufügen
    }

    @FXML
    private void onExecuteAll(ActionEvent event) {
        // TODO: alle Einträge aus batchTransactionTable ausführen
    }
}
