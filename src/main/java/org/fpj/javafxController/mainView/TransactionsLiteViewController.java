package org.fpj.javafxController.mainView;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.Data.InfinitePager;
import org.fpj.Data.UiHelpers;
import org.fpj.Exceptions.TransactionException;
import org.fpj.javafxController.ChatWindowController;
import org.fpj.javafxController.TransactionDetailController;
import org.fpj.payments.domain.TransactionResult;
import org.fpj.payments.domain.TransactionRow;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.domain.Transaction;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import static org.fpj.Data.UiHelpers.*;

@Getter
@Setter
@Component
public class TransactionsLiteViewController {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionService transactionService;

    @FXML
    private RadioButton rbDeposit;
    @FXML private RadioButton rbTransfer;
    @FXML private RadioButton rbWithdraw;

    @FXML private TextField tfEmpfaenger;
    @FXML private TextField tfBetrag;
    @FXML private TextField tfBetreff;

    @FXML private ListView<TransactionRow> lvTransactions;

    private final ObservableList<TransactionRow> liteTransactionList = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_Lite_List = 100;
    private static final int PAGE_PRE_FETCH_THRESHOLD = 50;

    private InfinitePager<TransactionRow> transactionPager;

    private User currentUser;
    private Consumer<String> balanceRefreshCallback;

    public void  initialize(User currentUser, Consumer<String> balanceRefreshCallback) {
        this.currentUser = currentUser;
        this.balanceRefreshCallback = balanceRefreshCallback;
        updateBalance();
        initTransactionList();
        initPager();
    }

    private void initPager() {
        long userId = currentUser.getId();

        this.transactionPager = new InfinitePager<>(
                PAGE_SIZE_Lite_List,
                (pageIndex, pageSize) -> transactionService.findLiteItemsForUser(
                        userId,
                        pageIndex,
                        pageSize
                ),
                page -> liteTransactionList.addAll(page.getContent()),
                ex -> showError("Transaktionen konnten nicht geladen werden: " +
                        (ex != null ? ex.getMessage() : "Unbekannter Fehler")),
                "trx-page-loader-"
        );

        liteTransactionList.clear();
        transactionPager.resetAndLoadFirstPage();
        setUpAutoCompletion();
    }

    private void loadTransactionsFirstPageLite() {
        liteTransactionList.clear();
        setUpAutoCompletion();
        this.transactionPager.resetAndLoadFirstPage();
    }

    private void updateBalance(){
        this.balanceRefreshCallback.accept(UiHelpers.formatEuro(this.transactionService.computeBalance(this.currentUser.getId())));
    }

    private void initTransactionList() {
        lvTransactions.setItems(this.liteTransactionList);

        lvTransactions.setCellFactory(list -> new ListCell<TransactionRow>() {
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
                            liteTransactionList.size(),
                            PAGE_PRE_FETCH_THRESHOLD
                    );

                    setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 2) {
                            openTransactionDetails(item);
                        }
                    });
                }
            }
        });
    }

    private void setUpAutoCompletion() {
        TextFields.bindAutoCompletion(tfEmpfaenger, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) return List.of();
            return userService.usernameContaining(term);
        });
    }

    @FXML
    private void onTypeChanged() {
        applyTypeVisibility();
    }

    private void applyTypeVisibility() {
        boolean isTransfer = rbTransfer.isSelected();
        tfEmpfaenger.setVisible(isTransfer);
        if (!isTransfer) {
            tfEmpfaenger.clear();
        }
    }

    @FXML
    private void sendTransfers() {
        try {
            BigDecimal amount = parseAmountTolerant(tfBetrag.getText());
            String subject = safe(tfBetreff.getText());

            TransactionResult result;
            if (rbDeposit.isSelected()) {
                result = transactionService.deposit(currentUser, amount, subject);
            } else if (rbWithdraw.isSelected()) {
                result = transactionService.withdraw(currentUser, amount, subject);
            } else if (rbTransfer.isSelected()) {
                String recipient = safe(tfEmpfaenger.getText());
                result = transactionService.transfer(currentUser, recipient, amount, subject);
            } else {
                throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
            }

            this.balanceRefreshCallback.accept(UiHelpers.formatEuro(result.newBalance()));

            Transaction t= result.transaction();
            Long sId = t.getSender()== null ? null : t.getSender().getId();
            Long rId = t.getRecipient()== null ? null : t.getRecipient().getId();

            String sName = t.getSender()== null ? null : t.getSender().getUsername();
            String rName = t.getRecipient()== null ? null : t.getRecipient().getUsername();

            TransactionRow row= new TransactionRow(t.getId(),t.getAmount(),t.getCreatedAt(), t.getTransactionType(),sId, sName, rId, rName, t.getDescription());
            addLiteTransaction(row);
            tfBetrag.clear();
            tfBetreff.clear();
            tfEmpfaenger.clear();
            updateBalance();
        } catch (TransactionException ex) {
            error("Transaktion fehlgeschlagen: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            error("Eingabe ungültig: " + ex.getMessage());
        } catch (Exception ex) {
            error("Unerwarteter Fehler: " + ex.getMessage());
        }
    }

    @FXML
    private void onReloadTransaction() {
        liteTransactionList.clear();
        if (transactionPager != null) {
            transactionPager.resetAndLoadFirstPage();
        }
        updateBalance();
    }

    private void openTransactionDetails(TransactionRow row){
        try {
            var url = getClass().getResource("/fxml/transaction_detail.fxml");
            if (url == null) {
                throw new IllegalStateException("chat_window.fxml nicht gefunden!");
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            TransactionDetailController detailController = loader.getController();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Transaktionsdetails");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

            detailController.initialize(row, currentUser, null, null, this::useTransactionAsTemplate, null, null);
        } catch (Exception e) {
            showError("Fehler beim laden der Transaktionsdetails. Versuche es erneut oder starte die Anwendung neu: " + e.getMessage());
        }
    }

    private void useTransactionAsTemplate(TransactionRow row) {
        tfBetrag.setText(row.amountStringUnsigned());
        tfBetreff.setText(row.description());
        switch (row.type()) {
            case UEBERWEISUNG:
                rbTransfer.setSelected(true);
                tfEmpfaenger.setText(row.recipientUsername());
                break;
            case AUSZAHLUNG: rbWithdraw.setSelected(true);
                break;
            case EINZAHLUNG: rbDeposit.setSelected(true);
        }
        onTypeChanged();
    }

    private void addLiteTransaction(TransactionRow  transactionItem) {
        this.liteTransactionList.add(0,transactionItem);
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
