package org.fpj.javafxController.mainView;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.Data.UiHelpers;
import org.fpj.Exceptions.TransactionException;
import org.fpj.payments.application.TransactionItemLite;
import org.fpj.payments.application.TransactionResult;
import org.fpj.payments.application.TransactionService;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import static org.fpj.Data.UiHelpers.parseAmountTolerant;
import static org.fpj.Data.UiHelpers.safe;

@Getter
@Setter
@Component
public class TransactionsLiteViewController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

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

    @FXML private ListView<TransactionItemLite> lvTransactions;

    private final ObservableList<TransactionItemLite> liteTransactionList = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_Lite_List = 100;
    private int currentPageLiteList = 0;
    private boolean lastPageLoadedLiteList = false;
    private boolean loadingNextPageLiteList = false;

    private User currentUser;
    private Consumer<String> balanceRefreshCallback;

    public void  initialize(User currentUser, Consumer<String> balanceRefreshCallback) {
        this.currentUser = currentUser;
        this.balanceRefreshCallback = balanceRefreshCallback;
        updateBalance();
        initTransactionList();
        loadTransactionsFirstPageLite();
    }

    private void addLiteTransaction(TransactionItemLite  transactionItem) {
        this.liteTransactionList.add(0,transactionItem);
    }

    private void loadTransactionsFirstPageLite() {
        currentPageLiteList = 0;
        lastPageLoadedLiteList = false;
        liteTransactionList.clear();
        loadNextPageLite();

        setUpAutoCompletion();
    }

    private void updateBalance(){
        this.balanceRefreshCallback.accept(UiHelpers.formatEuro(this.transactionService.computeBalance(this.currentUser.getId())));
    }

    private void initTransactionList() {
        lvTransactions.setItems(this.liteTransactionList);

        lvTransactions.setCellFactory(list -> new ListCell<TransactionItemLite>() {
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
            protected void updateItem(TransactionItemLite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    title.setText(item.counterparty());
                    subtitle.setText(TS.format(item.timestamp()) + "  •  " + UiHelpers.truncate(item.subject(), 20));
                    amount.setText(UiHelpers.formatSignedEuro(item.amount()));
                    amount.getStyleClass().removeAll("amt-pos", "amt-neg");
                    if (item.amount().signum() >= 0) {
                        amount.getStyleClass().add("amt-pos");
                    } else {
                        amount.getStyleClass().add("amt-neg");
                    }
                    setGraphic(root);
                    int index = getIndex();
                    ensureNextPageLoaded(index);
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

    // <editor-fold defaultstate="collapsed" desc="Infinite Scroll Transactions">
    private void ensureNextPageLoaded(int visibleIndex) {
        if (loadingNextPageLiteList || lastPageLoadedLiteList) {
            return;
        }

        int prefetchThreshold = 100;
        int size = liteTransactionList.size();

        if (visibleIndex >= size - prefetchThreshold) {
            loadNextPageLite();
        }
    }

    private void loadNextPageLite() {
        if (!canLoadNextLitePage()) {
            return;
        }

        loadingNextPageLiteList = true;

        int pageToLoad = currentPageLiteList;
        long userId = currentUser.getId();

        Task<Page<TransactionItemLite>> task = createLitePageTask(userId, pageToLoad);

        task.setOnSucceeded(ev -> onLitePageLoaded(task.getValue(), pageToLoad));
        task.setOnFailed(ev -> onLitePageFailed(task.getException()));

        startBackgroundTask(task, pageToLoad);
    }


    private boolean canLoadNextLitePage() {
        return !loadingNextPageLiteList && !lastPageLoadedLiteList;
    }

    private Task<Page<TransactionItemLite>> createLitePageTask(long userId, int pageToLoad) {
        return new Task<>() {
            @Override
            protected Page<TransactionItemLite> call() {
                return transactionService.findLiteItemsForUser(
                        userId,
                        pageToLoad,
                        PAGE_SIZE_Lite_List
                );
            }
        };
    }

    private void onLitePageLoaded(Page<TransactionItemLite> page, int pageToLoad) {
        try {
            liteTransactionList.addAll(page.getContent());
            lastPageLoadedLiteList = page.isLast();
            currentPageLiteList = pageToLoad + 1;
        } finally {
            loadingNextPageLiteList = false;
        }
    }

    private void onLitePageFailed(Throwable ex) {
        loadingNextPageLiteList = false;
        System.out.print(ex.toString());
        showError("Transaktionen konnten nicht geladen werden: " +
                (ex != null ? ex.getMessage() : "Unbekannter Fehler"));
    }

    private void startBackgroundTask(Task<?> task, int pageToLoad) {
        Thread t = new Thread(task, "trx-page-loader-" + pageToLoad);
        t.setDaemon(true);
        t.start();
    }
    // </editor-fold>

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
            addLiteTransaction(result.itemLite());

            tfBetrag.clear();
            tfBetreff.clear();
            tfEmpfaenger.clear();
        } catch (TransactionException ex) {
            error("Transaktion fehlgeschlagen: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            error("Eingabe ungültig: " + ex.getMessage());
        } catch (Exception ex) {
            error("Unerwarteter Fehler: " + ex.getMessage());
        }
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
