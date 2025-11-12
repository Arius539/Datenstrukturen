package org.fpj.javafxController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.fpj.Data.UiHelpers;
import org.fpj.messaging.application.ChatPreview;
import org.fpj.messaging.application.DirectMessageService;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.application.TransactionItemLite;
import org.fpj.payments.application.TransactionResult;
import org.fpj.Exceptions.TransactionException;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import java.util.Comparator;
import java.util.ArrayList;


import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class MainViewController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private UserService userService;

    // Profil/Saldo
    @FXML private Label lblEmail;
    @FXML private Label lblBalance;

    // Composer
    @FXML private RadioButton rbDeposit;
    @FXML private RadioButton rbTransfer;
    @FXML private RadioButton rbWithdraw;
    @FXML private TextField tfEmpfaenger;
    @FXML private TextField tfBetrag;
    @FXML private TextField tfBetreff;

    // Listen
    @FXML private ListView<TransactionItemLite> lvActivity;
    @FXML private ListView<ChatPreview>     lvChats;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private User currentUser;
    private final ObservableList<TransactionItemLite> activityItems = FXCollections.observableArrayList();
    private final ObservableList<ChatPreview> chatItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // User ermitteln
        currentUser = userService.currentUser();

        // Header befüllen
        lblEmail.setText(currentUser.getUsername());
        updateBalanceLabel();

        // Activity-List konfigurieren + Daten laden
        initActivityList();
        loadTransactionsFirstPage();

        // Chats konfigurieren + Daten laden
        initChatsList();
        loadChatPreviewsFirstPage();

    }

    /* ================= Composer Verhalten ================= */

    @FXML
    public void onTypeChanged() {
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
    public void sendTransfers() {
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

            // UI aktualisieren
            lblBalance.setText(UiHelpers.formatEuro(result.newBalance()));
            // Neueste Transaktion sichtbar machen
            activityItems.add(0, result.itemLite());

            // Eingaben zurücksetzen
            tfBetrag.clear();
            tfBetreff.clear();
            tfEmpfaenger.clear();

            info("Transaktion erfolgreich.");
        } catch (TransactionException ex) {
            error("Transaktion fehlgeschlagen: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            error("Eingabe ungültig: " + ex.getMessage());
        } catch (Exception ex) {
            error("Unerwarteter Fehler: " + ex.getMessage());
        }
    }


    @FXML public void actionTransactions()    { }
    @FXML public void actionWallComments()    { info("Navigation: Wall Kommentare (Placeholder)."); }
    @FXML public void actionDirectMessages()  { info("Navigation: Massen Transaktion (Placeholder)."); }

    private void initActivityList() {
        lvActivity.setItems(activityItems);

        lvActivity.setCellFactory(list -> new ListCell<TransactionItemLite>() {
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
                    subtitle.setText(TS.format(item.timestamp()) + "  •  " + UiHelpers.truncate(item.subject(), 64));
                    amount.setText(UiHelpers.formatSignedEuro(item.amount()));
                    amount.getStyleClass().removeAll("amt-pos", "amt-neg");
                    if (item.amount().signum() >= 0) {
                        amount.getStyleClass().add("amt-pos");
                    } else {
                        amount.getStyleClass().add("amt-neg");
                    }
                    setGraphic(root);
                }
            }
        });
    }

    private void loadTransactionsFirstPage() {
        activityItems.clear();
        var page = transactionService.findLiteItemsForUser(currentUser.getId(), 0, 50);
        page.getContent().forEach(activityItems::add);
    }

    private void initChatsList() {
        lvChats.setItems(chatItems);

        lvChats.setCellFactory(list -> new ListCell<ChatPreview>() {
            private final Label title = new Label();
            private final Label subtitle = new Label();
            private final VBox left = new VBox(2, title, subtitle);
            private final Label ts = new Label();
            private final Region spacer = new Region();
            private final HBox root = new HBox(8, left, spacer, ts);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(ChatPreview item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                title.setText(item.name());

                String msg = item.lastMessage();
                if (msg == null || msg.isBlank()) {
                    subtitle.setText("Noch keine Nachrichten");
                } else {
                    subtitle.setText(UiHelpers.truncate(msg, 64));
                }

                ts.setText(item.timestamp() == null ? "" : TS.format(item.timestamp()));

                setGraphic(root);
            }
        });
    }

    private void loadChatPreviewsFirstPage() {
        chatItems.clear();
        try {
            var page = directMessageService.getChatPreviews(currentUser, PageRequest.of(0, 50));
            var list = new ArrayList<>(page.getContent());

            // Neueste Chats oben; Chats ohne Timestamp zuletzt
            list.sort(Comparator
                    .comparing(ChatPreview::timestamp,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed());

            chatItems.addAll(list);
        } catch (Exception ex) {
            error("Chats konnten nicht geladen werden: " + ex.getMessage());
        }
    }


    /* ================= Utils & Modelle ================= */

    private void updateBalanceLabel() {
        var balance = transactionService.computeBalance(currentUser.getId());
        lblBalance.setText(UiHelpers.formatEuro(balance));
    }

    private static BigDecimal parseAmountTolerant(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Betrag ist erforderlich.");
        }

        String s = raw.replace("€","").replaceAll("[\\s\\u00A0]", "").trim();
        int lastComma = s.lastIndexOf(',');
        int lastDot   = s.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.');
        }

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Betrag konnte nicht gelesen werden.");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
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
