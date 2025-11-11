package org.fpj.javafxController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MainViewController {

    @Autowired
    private ApplicationContext context;

    // Profil/Saldo
    @FXML private Label lblEmail;
    @FXML private Label lblBalance;

    // Composer
    @FXML private RadioButton rbDeposit;   // Einzahlen
    @FXML private RadioButton rbTransfer;  // Überweisen
    @FXML private RadioButton rbWithdraw;  // Auszahlen
    @FXML private TextField tfEmpfaenger;
    @FXML private TextField tfBetrag;
    @FXML private TextField tfBetreff;

    // Listen
    @FXML private ListView<TransactionItem> lvActivity;    // Letzte Transaktionen
    @FXML private ListView<ChatPreview>     lvChats;       // Chats

    private static final double GAP = 12.0;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    public void initialize() {
        // Demo-Daten – später durch Services ersetzen
        lblEmail.setText("demo_user@example.com");
        lblBalance.setText("€ 1.234,56");

        // Composer: Empfänger-Feld managed an visible binden, Startzustand
        tfEmpfaenger.managedProperty().bind(tfEmpfaenger.visibleProperty());
        applyTypeVisibility();

        // Letzte Transaktionen (Empfänger/Sender, Betrag, Datum, Betreff)
        ObservableList<TransactionItem> txs = FXCollections.observableArrayList(
                // Betrag < 0 => Geld geht raus => Empfänger anzeigen
                new TransactionItem("Bob",  new BigDecimal("-15.00"), LocalDateTime.now().minusHours(2),  "Kaffee"),
                // Betrag > 0 => Geld kommt rein => Sender anzeigen
                new TransactionItem("Alice",new BigDecimal("50.00"),  LocalDateTime.now().minusDays(1).withHour(18).withMinute(2), "Rückzahlung"),
                new TransactionItem("Clara",new BigDecimal("-20.00"), LocalDateTime.now().minusDays(1).withHour(9).withMinute(14), "Taxi")
        );
        lvActivity.setItems(txs);
        lvActivity.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(TransactionItem it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) { setGraphic(null); setText(null); return; }

                String counterpartyLabel = it.amount.signum() < 0 ? it.counterparty : it.counterparty; // Name anzeigen
                String amountStr = formatAmount(it.amount);
                String dateStr   = TS.format(it.timestamp);
                String subject   = it.subject == null ? "" : it.subject;

                Label lCounterparty = new Label(counterpartyLabel);
                Label lAmount       = new Label(amountStr);
                Label lDate         = new Label(dateStr);
                Label lSubject      = new Label(subject);

                HBox row = new HBox(GAP, lCounterparty, lAmount, lDate, lSubject);
                row.setPadding(new Insets(6));
                setGraphic(row);
                setText(null);
            }
        });

        // Chats (Name, letzte Nachricht (20 Zeichen), Datum)
        ObservableList<ChatPreview> chats = FXCollections.observableArrayList(
                new ChatPreview("Alice", "Alles gut bei dir? Wir sehen uns später im Büro.", LocalDateTime.now().minusMinutes(10)),
                new ChatPreview("Bob",   "Danke für die Zahlung.",                         LocalDateTime.now().minusHours(1)),
                new ChatPreview("Clara", "Bis morgen!",                                     LocalDateTime.now().minusDays(1))
        );
        lvChats.setItems(chats);
        lvChats.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ChatPreview it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) { setGraphic(null); setText(null); return; }

                Label lName  = new Label(it.name);
                Label lMsg   = new Label(truncate(it.lastMessage, 20));
                Label lDate  = new Label(TS.format(it.timestamp));

                HBox row = new HBox(GAP, lName, lMsg, lDate);
                row.setPadding(new Insets(6));
                setGraphic(row);
                setText(null);
            }
        });

        // Klick: Chat-FXML öffnen
        lvChats.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            if (sel != null) {
                openChatWindow(sel.name);
                lvChats.getSelectionModel().clearSelection();
            }
        });
    }

    /* ================= Composer Verhalten ================= */

    @FXML
    public void onTypeChanged() {
        applyTypeVisibility();
    }

    private void applyTypeVisibility() {
        boolean isTransfer = rbTransfer.isSelected();
        tfEmpfaenger.setVisible(isTransfer); // bei Einzahlen/Auszahlen unsichtbar
        if (!isTransfer) {
            tfEmpfaenger.clear();
        }
    }

    @FXML
    public void sendTransfers() {
        String type = rbDeposit.isSelected() ? "Einzahlen" : rbWithdraw.isSelected() ? "Auszahlen" : "Überweisen";
        String empf = tfEmpfaenger.isVisible() ? tfEmpfaenger.getText() : "(kein Empfänger)";
        String betrag = tfBetrag.getText();
        String betreff = tfBetreff.getText();

        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Typ: " + type + "\nEmpfänger: " + empf + "\nBetrag: " + betrag + "\nBetreff: " + betreff,
                ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    /* ================= Actions (Buttons links) ================= */

    @FXML public void actionTransactions()    { info("Navigation: Transaktionen (Placeholder)."); }
    @FXML public void actionWallComments()    { info("Navigation: Wall Kommentare (Placeholder)."); }
    @FXML public void actionDirectMessages() { info("Navigation: Massen Transaktion (Placeholder)."); }

    /* ================= Chat-Fenster (aus FXML) ================= */

    private void openChatWindow(String contact) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_window.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            ChatWindowController ctrl = loader.getController();
            ctrl.init(contact);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Chat mit " + contact);
            dialog.setScene(new javafx.scene.Scene(root, 480, 420));
            dialog.showAndWait();

        } catch (Exception e) {
            throw new RuntimeException("Chat-Fenster konnte nicht geöffnet werden", e);
        }
    }

    /* ================= Utils & Modelle ================= */

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String formatAmount(BigDecimal amt) {
        String sign = amt.signum() < 0 ? "-" : "+";
        BigDecimal abs = amt.abs();
        return sign + " € " + abs.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private void info(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    public record TransactionItem(String counterparty, BigDecimal amount, LocalDateTime timestamp, String subject) {}
    public record ChatPreview(String name, String lastMessage, LocalDateTime timestamp) {}
}
