package org.fpj.javafxController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MainViewController {
    @Autowired
    private ApplicationContext context;

    // Profil/Saldo
    @FXML private Label lblEmail;
    @FXML private Label lblBalance;

    // Wall / Feed
    @FXML private ListView<WallItem> lvWallComments;
    @FXML private ListView<ActivityItem> lvActivity;

    // Composer (zentral)
    @FXML private VBox vbTransferRows;

    // Chats (rechte Spalte)
    @FXML private ListView<ChatPreview> lvChats;

    private static final double GAP = 12.0;

    @FXML
    public void initialize() {
        // Demo-Daten (später durch Services ersetzen)
        lblEmail.setText("demo_user@example.com");
        lblBalance.setText("€ 1.234,56");

        // --- Activity / Überweisungen (Zeit, Typ, Betrag) ---
        ObservableList<ActivityItem> activities = FXCollections.observableArrayList(
                new ActivityItem("11.11.2025 10:23", "Senden",  "€ 15,00"),
                new ActivityItem("10.11.2025 18:02", "Einzahlung", "€ 50,00"),
                new ActivityItem("10.11.2025 09:14", "Auszahlung", "€ 20,00")
        );
        lvActivity.setItems(activities);
        lvActivity.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ActivityItem it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label lTime = new Label(it.time());
                    Label lType = new Label(it.type());
                    Label lAmount = new Label(it.amount());
                    HBox row = new HBox(GAP, lTime, lType, lAmount);
                    row.setPadding(new Insets(6));
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // --- Wall-Kommentare (Zeit, User, Snippet) ---
        ObservableList<WallItem> wall = FXCollections.observableArrayList(
                new WallItem("11.11.2025 11:00", "Alice", "Willkommen auf meiner Wall!"),
                new WallItem("10.11.2025 17:40", "Bob",   "Überweisung erhalten, danke."),
                new WallItem("10.11.2025 09:05", "Clara", "Treffen wir uns morgen?")
        );
        lvWallComments.setItems(wall);
        lvWallComments.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(WallItem it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label lTime = new Label(it.time());
                    Label lUser = new Label(it.user());
                    Label lMsg  = new Label(it.text());
                    HBox row = new HBox(GAP, lTime, lUser, lMsg);
                    row.setPadding(new Insets(6));
                    row.setFillHeight(true);
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // --- Chats (Zeit, Kontakt, letzte Nachricht) ---
        ObservableList<ChatPreview> chats = FXCollections.observableArrayList(
                new ChatPreview("11.11.2025 12:05", "Alice", "Alles gut?"),
                new ChatPreview("11.11.2025 10:30", "Bob",   "Danke für die Zahlung."),
                new ChatPreview("10.11.2025 19:10", "Clara", "Bis morgen!")
        );
        lvChats.setItems(chats);
        lvChats.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ChatPreview it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label lTime = new Label(it.time());
                    Label lUser = new Label(it.contact());
                    Label lMsg  = new Label(it.lastMessage());
                    HBox row = new HBox(GAP, lTime, lUser, lMsg);
                    row.setPadding(new Insets(6));
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // Klick öffnet ein separates Chat-Fenster
        lvChats.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            if (sel != null) {
                openChatWindow(sel.contact());
                // Selektion zurücksetzen, damit erneutes Klicken wieder öffnet
                lvChats.getSelectionModel().clearSelection();
            }
        });

        // Composer initiale Zeile
        addTransferRow();
    }

    @FXML
    public void addTransferRow() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(60);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(40);
        grid.getColumnConstraints().addAll(c1, c2);

        TextField tfEmpfaenger = new TextField();
        tfEmpfaenger.setPromptText("Empfänger");
        TextField tfBetrag = new TextField();
        tfBetrag.setPromptText("Betrag (z. B. 12,34)");

        grid.add(tfEmpfaenger, 0, 0);
        grid.add(tfBetrag,     1, 0);

        TextField tfGrund = new TextField();
        tfGrund.setPromptText("Grund / Text / Betreff");

        VBox container = new VBox(6, grid, tfGrund);
        container.getStyleClass().add("composer-row");
        container.setPadding(new Insets(8));

        vbTransferRows.getChildren().add(container);
    }

    @FXML
    public void removeTransferRow() {
        int n = vbTransferRows.getChildren().size();
        if (n > 0) vbTransferRows.getChildren().remove(n - 1);
    }

    @FXML
    public void sendTransfers() {
        // Placeholder: reine UI-Bestätigung
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Transfers würden jetzt gesendet.", ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    @FXML
    public void openWallComments() {
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Navigation zur vollständigen Wall-Ansicht (Placeholder).", ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    @FXML
    public void openTransactionOverview() {
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Navigation zur Transaktionsübersicht (Placeholder).", ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    private void openChatWindow(String contact) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_window.fxml"));
            loader.setControllerFactory(context::getBean); // Spring-basiert
            Parent root = loader.load();

            ChatWindowController ctrl = loader.getController();
            ctrl.init(contact); // Parameter an "Page/Slice"-Controller

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Chat mit " + contact);
            dialog.setScene(new javafx.scene.Scene(root, 480, 420));
            dialog.showAndWait();

        } catch (Exception e) {
            throw new RuntimeException("Chat-Fenster konnte nicht geöffnet werden", e);
        }
    }

    private void wireChatList() {
        lvChats.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            if (sel != null) {
                openChatWindow(sel.contact());
                lvChats.getSelectionModel().clearSelection();
            }
        });
    }

    public record ActivityItem(String time, String type, String amount) {}
    public record WallItem(String time, String user, String text) {}
    public record ChatPreview(String time, String contact, String lastMessage) {}
}
