package org.fpj.javafxController;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.Data.InfinitePager;
import org.fpj.Data.UiHelpers;
import org.fpj.Exceptions.TransactionException;
import org.fpj.exportImport.adapter.FileHandling;
import org.fpj.exportImport.application.MassTransferCsvReader;
import org.fpj.exportImport.application.TransactionCsvExporter;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.domain.*;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Component
public class TransactionViewController {
    TransactionCsvExporter  transactionCsvExporter = new TransactionCsvExporter();

    @FXML
    public Label balanceLabelBatch;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    // --- Linke Seite: Kontostände, Filter, Transaktionsliste ---
    @FXML
    private Label currentBalanceLabel;

    @FXML
    private TextField filterTextField;

    @FXML
    private Label selectedTransactionBalanceLabel;

    @FXML
    private ComboBox<String> filterFieldComboBox;

    private String beforeActionComboBoxValue;

    @FXML
    private Button clearFilterButton;

    @FXML
    private Button searchButton;

    @FXML
    private ListView<TransactionRow> transactionTable;




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
    private ListView<TransactionLite> batchTransactionTable;

    @FXML
    private Button executeAllButton;

    private User currentUser;

    private TransactionViewSearchParameter searchParameter;

    private final ObservableList<TransactionRow> transactionList = FXCollections.observableArrayList();

    private final ObservableList<TransactionLite> batchTransactionList = FXCollections.observableArrayList();

    private static final int PAGE_SIZE_List = 100;
    private static final int PAGE_PRE_FETCH_THRESHOLD = 100;

    private InfinitePager<TransactionRow> transactionPager;

    //Für das Suchfeld wenn der Filter Empfänger Sender ausgewählt ist
    private AutoCompletionBinding<String> autoCompletionBinding = null;


    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    public void initialize(User currentUser, TransactionViewSearchParameter searchParameter) {
        this.currentUser = currentUser;
        this.searchParameter = searchParameter;
        processSearchParameter();
        initUiElements();
        initTransactionList();
        initBatchTransactionList();
        initPager();
        updateBalances();
        setUpAutoCompletion();
    }

    private void setUpAutoCompletion() {
        TextFields.bindAutoCompletion(receiverUsernameField, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) return List.of();
            return userService.usernameContaining(term);
        });
    }

    private void updateBalances(){
        updateCurrentBalanceLabel();
        updateBatchTransactionBalanceLabel();
    }

    private void updateCurrentBalanceLabel(){
      this.currentBalanceLabel.setText( UiHelpers.formatEuro(this.transactionService.computeBalance(this.currentUser.getId())));
    }

    private void updateSelectedBalanceLabel(BigDecimal amount){
        this.selectedTransactionBalanceLabel.setText( UiHelpers.formatEuro(amount));
    }

    private BigDecimal getBalanceAfterListOfItems(List<TransactionLite> transactionList){
        BigDecimal currentBalance = this.transactionService.computeBalance(this.currentUser.getId());
        for (TransactionLite transactionLite : transactionList){
            currentBalance = transactionLite.isOutgoing(currentUser.getUsername()) ?currentBalance.subtract(transactionLite.amount()):  currentBalance.add(transactionLite.amount());
        }
        return currentBalance;
    }

    private void updateBatchTransactionBalanceLabel(){
        BigDecimal currentBalance = this.transactionService.computeBalance(this.currentUser.getId());
        for (TransactionLite transactionLite : batchTransactionList){
            currentBalance = transactionLite.isOutgoing(currentUser.getUsername()) ?currentBalance.subtract(transactionLite.amount()):  currentBalance.add(transactionLite.amount());
        }
        this.balanceLabelBatch.setText(UiHelpers.formatSignedEuro(getBalanceAfterListOfItems(batchTransactionList)));
    }

    private boolean executeTransactionByList(List<TransactionLite> transactionList){
        try{
            BigDecimal balance= getBalanceAfterListOfItems(transactionList);
            if(balance.compareTo(BigDecimal.ZERO)< 0){
                throw new TransactionException("Transaktionsausführung ist nicht möglich. Dein Kontostand würde unter 0 fallen");
            }
            for (TransactionLite transactionLite : transactionList){
                sendTransfers(transactionLite);
            }
            return true;
        } catch (TransactionException e) {
            error("Transaktionsfehler: "+e.getMessage());
        } catch (Exception e){
            error("Unerwarteter Fehler: "+ e.getMessage());
        }
        return false;
    }

   private void initUiElements(){
       if (batchTransactionTable != null) {
           batchTransactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
       }

       if (transactionTypeToggleGroup != null && transferRadio != null) {
           transactionTypeToggleGroup.selectToggle(transferRadio);
       }
    }

    private void processSearchParameter() {
        if(this.searchParameter==null) this.searchParameter = new TransactionViewSearchParameter(null,null, null, null, null, null, null);
        //TODO eine close fenster methode bei dem error auslösen
        if(this.currentUser==null) error("Fehler beim laden der nötigen Daten, bitte start die Anwendung neu");
        this.searchParameter.setCurrentUserID(this.currentUser.getId());

    }

    private void reloadTransactionList(){
        transactionList.clear();
        transactionPager.resetAndLoadFirstPage();
    }

    private void initPager() {
        this.transactionPager = new InfinitePager<>(
                PAGE_SIZE_List,
                (pageIndex, pageSize) -> transactionService.searchTransactions(
                        this.searchParameter,
                        pageIndex,
                        pageSize
                ),
                page -> transactionList.addAll(page.getContent()),
                ex ->  {showError("Transaktionen konnten nicht geladen werden: " +
                        (ex != null ? ex.getMessage() : "Unbekannter Fehler"));
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                    },
                "trx-page-loader-"
        );

        transactionList.clear();
        transactionPager.resetAndLoadFirstPage();
    }
    private void initBatchTransactionList(){
        batchTransactionTable.setItems(batchTransactionList);
        batchTransactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        batchTransactionTable.setCellFactory(list -> new ListCell<TransactionLite>() {
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
            protected void updateItem(TransactionLite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean outgoing = Objects.equals(item.senderUsername(), currentUser.getUsername());

                    String name = outgoing
                            ? (item.recipientUsername() != null ? item.recipientUsername() : "Empfänger unbekannt")
                            : (item.senderUsername() != null ? item.senderUsername() : "Sender unbekannt");

                    String counterparty = switch (item.type()) {
                        case EINZAHLUNG   -> "Einzahlung";
                        case AUSZAHLUNG   -> "Auszahlung";
                        case UEBERWEISUNG -> (outgoing ? "Überweisung an " : "Überweisung von ") + name;
                    };

                    title.setText(counterparty);
                    subtitle.setText(UiHelpers.truncate(item.description(), 30));
                    amount.setText(item.amountString(currentUser.getUsername()));
                    setGraphic(root);


                    setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 1) {
                        }

                        if (ev.getClickCount() == 2) {
                            openTransactionDetails(item);
                        }
                    });
                }
            }
        });
    }

    private void initTransactionList(){
        transactionTable.setItems(transactionList);

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
                    transactionPager.ensureLoadedForIndex(
                            index,
                            transactionList.size(),
                            PAGE_PRE_FETCH_THRESHOLD
                    );

                    setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 1) {
                            updateSelectedBalanceLabel(transactionService.findUserBalanceAfterTransaction(currentUser.getId(), item.id()));
                        }

                        if (ev.getClickCount() == 2) {
                           openTransactionDetails(TransactionLite.fromTransactionRow(item));
                        }
                    });
                }
            }
        });
    }

    private void openTransactionDetails(TransactionLite row){
        try {
            var url = getClass().getResource("/fxml/transaction_detail.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            TransactionDetailController detailController = loader.getController();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Transaktionsdetails");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
            //TODO Implement die Callbacks um dann so zu suchen
            detailController.initialize(row, currentUser, null, null, this::useTransactionAsTemplate, null, null);
        } catch (Exception e) {
            showError("Fehler beim laden der Transaktionsdetails. Versuche es erneut oder starte die Anwendung neu: " + e.getMessage());
        }
    }

    private TransactionLite transactionInfosToTransactionLite() {
        try {
            String amount = amountField.getText();
            String subject = purposeField.getText();
            String recipient = receiverUsernameField.getText();
            String sender= null;
            TransactionType type = null;
            if (depositRadio.isSelected()) {
                sender= null;
                recipient = currentUser.getUsername();
                type = TransactionType.EINZAHLUNG;
            } else if (withdrawRadio.isSelected()) {
                sender = currentUser.getUsername();
                recipient = null;
                type = TransactionType.AUSZAHLUNG;
            } else if (transferRadio.isSelected()) {
                sender = currentUser.getUsername();
                UiHelpers.isValidEmail(recipient);
                type = TransactionType.UEBERWEISUNG;
            } else {
                throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
            }
            return transactionService.transactionInfosToTransactionLite(amount, sender, recipient, subject, type);
        } catch (TransactionException ex) {
            error("Transaktion fehlgeschlagen: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            error("Eingabe ungültig: " + ex.getMessage());
        } catch (Exception ex) {
            error("Unerwarteter Fehler: " + ex.getMessage());
        }
        return null;
    }

    private void sendTransfers(TransactionLite transactionLite) {
        try {
            TransactionResult result= transactionService.sendTransfers(transactionLite, currentUser);
            TransactionRow row= TransactionRow.fromTransaction(result.transaction());
            this.transactionList.add(0, row);
            updateBalances();
        } catch (TransactionException ex) {
            error("Transaktion fehlgeschlagen: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            error("Eingabe ungültig: " + ex.getMessage());
        } catch (Exception ex) {
            error("Unerwarteter Fehler: " + ex.getMessage());
        }
    }

    private void useTransactionAsTemplate(TransactionLite row) {
        amountField.setText(row.amountStringUnsigned());
        purposeField.setText(row.description());
        switch (row.type()) {
            case UEBERWEISUNG:
                transferRadio.setSelected(true);
                receiverUsernameField.setText(row.recipientUsername());
                break;
            case AUSZAHLUNG: withdrawRadio.setSelected(true);
                break;
            case EINZAHLUNG: depositRadio.setSelected(true);
        }
        applyTypeVisibility();
    }

    private boolean parseValuesFromSearchField( String selected ){
        try {
            switch (selected) {
                case "Verwendungszweck":
                    if(this.filterTextField.getText().isEmpty()) this.searchParameter.setDescription(null);
                    this.searchParameter.setDescription(this.filterTextField.getText());
                    break;
                case "Empfänger, Sender":
                    if(this.filterTextField.getText().isEmpty()) this.searchParameter.setSenderRecipientUsername(null);
                    this.searchParameter.setSenderRecipientUsername(this.filterTextField.getText());
                    break;
                case "Created at von":
                    if(this.filterTextField.getText().isEmpty()) this.searchParameter.setCreatedFrom(null);
                    this.searchParameter.setCreatedFrom(
                            UiHelpers.parseDateTolerant(this.filterTextField.getText())
                    );
                    break;
                case "Created at bis":
                    if(this.filterTextField.getText().isEmpty()) this.searchParameter.setCreatedTo(null);
                    this.searchParameter.setCreatedTo(
                            UiHelpers.parseDateTolerant(this.filterTextField.getText())
                    );
                    break;
                case "Betrag ab":
                    if(this.filterTextField.getText().isEmpty()) this.searchParameter.setAmountFrom(null);
                    this.searchParameter.setAmountFrom(
                            UiHelpers.parseAmountTolerant(this.filterTextField.getText()).abs()
                    );
                    break;
                case "Betrag bis":
                    if(this.filterTextField.getText().isEmpty()) this.searchParameter.setAmountTo(null);
                    this.searchParameter.setAmountTo(
                            UiHelpers.parseAmountTolerant(this.filterTextField.getText()).abs()
                    );
                    break;
            }
        } catch (Exception e) {
            if(!this.filterTextField.getText().isEmpty())error("Es ist ein Fehler beim Lesen des Filterwertes aufgetreten: " + e.getMessage());
            return false;
        }
        return true;
    }

    private String getTextValueSelectedFilter(String selected){
        try {
            if (this.searchParameter == null) {
                return "";
            }
            switch (selected) {
                case "Verwendungszweck":
                    String description = this.searchParameter.getDescription();
                    return description != null ? description : "";

                case "Empfänger, Sender":
                    String senderRecipient = this.searchParameter.getSenderRecipientUsername();
                    return senderRecipient != null ? senderRecipient : "";

                case "Created at von":
                    Instant createdFrom = this.searchParameter.getCreatedFrom();
                    return createdFrom != null ? UiHelpers.formatInstantToDate(createdFrom) : "";

                case "Created at bis":
                    Instant createdTo = this.searchParameter.getCreatedTo();
                    return createdTo != null ? UiHelpers.formatInstantToDate(createdTo) : "";

                case "Betrag ab":
                    BigDecimal amountFrom = this.searchParameter.getAmountFrom();
                    return amountFrom != null ? UiHelpers.formatBigDecimal(amountFrom) : "";

                case "Betrag bis":
                    BigDecimal amountTo = this.searchParameter.getAmountTo();
                    return amountTo != null ? UiHelpers.formatBigDecimal(amountTo) : "";

                default:
                    return "";
            }
        } catch (Exception e) {
            throw new RuntimeException();
            //return "";
        }
    }

    private void applyTypeVisibility() {
        boolean isTransfer = transferRadio.isSelected();
        receiverUsernameField.setVisible(isTransfer);
        if (!isTransfer) {
            receiverUsernameField.clear();
        }
    }

    @FXML
    private void onReloadTransactions(ActionEvent event){
        transactionList.clear();
        transactionPager.resetAndLoadFirstPage();
    }

    @FXML
    private void onReloadBatches(ActionEvent event){
        updateBalances();
    }
    // ---------------------------------------------------------------
    // Aktionen: Filter / Suche (linke Seite)
    // ---------------------------------------------------------------

    private void updateAutoCompletion(boolean enable) {
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
            autoCompletionBinding = null;
        }

        if (enable) {
            autoCompletionBinding = TextFields.bindAutoCompletion(
                    filterTextField,
                    request -> {
                        String term = request.getUserText();
                        if (term == null || term.isBlank()) return List.of();
                        return userService.usernameContaining(term);
                    }
            );
        }
    }
    @FXML
    private void onFilterChanged(ActionEvent event) {
        String selected = filterFieldComboBox.getValue();
        if(beforeActionComboBoxValue!= null) parseValuesFromSearchField(beforeActionComboBoxValue);
        String filterText= getTextValueSelectedFilter(selected);
       filterTextField.setText(filterText);
       beforeActionComboBoxValue = selected;
       updateAutoCompletion(selected.equals("Empfänger, Sender"));
    }

    @FXML
    private void onClearFilter(ActionEvent event) {
        filterTextField.setText("");
        filterFieldComboBox.getSelectionModel().clearSelection();
        this.searchParameter=null;
        processSearchParameter();
        reloadTransactionList();
    }

    @FXML
    private void onSearch(ActionEvent event) {
        String selected = filterFieldComboBox.getValue();
        if (selected == null) {
            return;
        }
        parseValuesFromSearchField(selected);
        reloadTransactionList();
    }


    // ---------------------------------------------------------------
    // Aktionen: Kontextmenü der Tabellen
    // (transactionTable & batchTransactionTable)
    // ---------------------------------------------------------------

    @FXML
    private void onDeleteTransaction(ActionEvent event) {
        List<TransactionLite> selectedTransactions = new ArrayList<>(batchTransactionTable.getSelectionModel().getSelectedItems());
        if (selectedTransactions.isEmpty()) {
            return;
        }
        for (TransactionLite transactionLite : selectedTransactions) {
            batchTransactionList.remove(transactionLite);
        }
        batchTransactionTable.getSelectionModel().clearSelection();
        updateBalances();
    }

    @FXML
    private void onExecuteSingleFromContext(ActionEvent event) {
        List<TransactionLite> selectedTransactions = new ArrayList<>(batchTransactionTable.getSelectionModel().getSelectedItems());
        batchTransactionTable.getSelectionModel().clearSelection();
        boolean result= executeTransactionByList(selectedTransactions);
        if(result)for (TransactionLite transactionLite : selectedTransactions) {
            batchTransactionList.remove(transactionLite);
        }
        updateBalances();
    }

    private void openCsvImportDialog(){
        try {
            var url = getClass().getResource("/fxml/csvImportDialog.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            CsvImportDialogController<MassTransfer> transferCsvImportDialogController = loader.getController();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Massenüberweisung Import");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
            //TODO Implement die Callbacks um dann so zu suchen
            MassTransferCsvReader reader= new MassTransferCsvReader();
            reader.setCurrentUser(this.currentUser);
            reader.setUserService(this.userService);
            transferCsvImportDialogController.initialize(reader, this::addTransactionToBatch);
        } catch (Exception e) {
            showError("Fehler beim laden der Transaktionsdetails. Versuche es erneut oder starte die Anwendung neu: " + e.getMessage());
        }
    }

    private void addTransactionToBatch(List< MassTransfer> massTransfers){
        for (MassTransfer massTransfer : massTransfers) {
            this.batchTransactionList.add(new TransactionLite(massTransfer.betrag(),TransactionType.UEBERWEISUNG, this.currentUser.getUsername(), massTransfer.empfaenger(), massTransfer.beschreibung()));
        }
        this.updateBalances();
    }


    // ---------------------------------------------------------------
    // Aktionen: RadioButtons (Transaktionstyp)
    // ---------------------------------------------------------------

    @FXML
    private void onTransactionTypeChanged(ActionEvent event) {
        applyTypeVisibility();
    }


    // ---------------------------------------------------------------
    // Aktionen: Buttons rechts
    // ---------------------------------------------------------------
    @FXML
    private void exportTransactions(){
        try {
            if(transactionCsvExporter.isRunning()) {
                showError("Ein andere Exporter instanz läuft noch. Warte bitte bis diese abgeschlossen ist.");
                return;
            }
            Window window = importCsvButton.getScene().getWindow();
            String path = FileHandling.openFileChooserAndGetPath(window);
            if (path == null) {
                showError("Das auswählen des Paths ist fehlgeschlagen");
                return;
            }
            List<TransactionRow> messages = transactionService.transactionsForUserAsList(currentUser.getId());
            transactionCsvExporter.export(messages.iterator(),FileHandling.openFileAsOutStream(path));
            info("Der Export der Transaktionen war erfolgreich. Du findest die Einträge in: "+path);
        }catch (IllegalArgumentException e){
            showError("Fehler beim exportieren der Nachrichten: " + e.getMessage());
        } catch (Exception e) {
            showError("Ein Unbekannter Fehler ist aufgetreten: " + e.getMessage());
        }
    }

    @FXML
    private void onImportCsv(ActionEvent event) {
        openCsvImportDialog();
        updateBalances();
    }

    @FXML
    private void onExecuteSingle(ActionEvent event) {
        TransactionLite transactionLite= this.transactionInfosToTransactionLite();
        if(transactionLite==null) return;
        List<TransactionLite> list = new ArrayList<>();
        list.add(transactionLite);
        executeTransactionByList(list);
        updateBalances();
    }

    @FXML
    private void onAddToBatch(ActionEvent event) {
        TransactionLite transactionLite= this.transactionInfosToTransactionLite();
        if(transactionLite==null) return;
        batchTransactionList.add(transactionLite);
        updateBalances();
    }

    @FXML
    private void onExecuteAll(ActionEvent event) {
        boolean result= executeTransactionByList(batchTransactionList);
        if(result) batchTransactionList.clear();
        updateBalances();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
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
