package org.fpj.javafxController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.users.domain.User;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class TransactionDetailController {

    @FXML
    private StackPane senderBox;

    @FXML
    private StackPane empfaengerBox;

    @FXML
    private StackPane betragBox;

    @FXML
    private StackPane verwendungszweckBox;

    @FXML
    private Label senderLabel;

    @FXML
    private Label empfaengerLabel;

    @FXML
    private Label betragLabel;

    @FXML
    private Label verwendungszweckLabel;

    private TransactionLite transaction;
    private Consumer<TransactionLite> onSenderClicked;
    private Consumer<TransactionLite> onRecipientClicked;
    private Consumer<TransactionLite> onReuseClicked;
    private Consumer<TransactionLite> onDescriptionClicked;
    private Consumer<TransactionLite> onValueClicked;
    private User currentUser;

    // <editor-fold defaultstate="collapsed" desc="initialize">
    @FXML
    private void initialize() {
    }

    public void initialize(TransactionLite transaction, User currentUser, Consumer<TransactionLite> onSenderClicked, Consumer<TransactionLite> onRecipientClicked, Consumer<TransactionLite> onReuseClicked, Consumer<TransactionLite> onDescriptionClicked, Consumer<TransactionLite> onValueClicked) {
        this.currentUser = currentUser;
        this.transaction = transaction;
        this.onSenderClicked = onSenderClicked;
        this.onRecipientClicked = onRecipientClicked;
        this.onReuseClicked = onReuseClicked;
        this.onDescriptionClicked = onDescriptionClicked;
        this.onValueClicked = onValueClicked;

        updateView();
        updateClickability();
    }
    // </editor-fold>

    @FXML
    private void onSenderClicked(MouseEvent event) {
        handleAndClose(event.getSource(), onSenderClicked);
    }

    @FXML
    private void onRecipientClicked(MouseEvent event) {
        handleAndClose(event.getSource(), onRecipientClicked);
    }

    @FXML
    private void onValueClicked(MouseEvent event) {
        handleAndClose(event.getSource(), onValueClicked);
    }

    @FXML
    private void onDescriptionClicked(MouseEvent event) {
        handleAndClose(event.getSource(), onDescriptionClicked);
    }

    @FXML
    private void onReuseClicked(ActionEvent event) {
        handleAndClose(event.getSource(), onReuseClicked);
    }

    private void updateView() {
        if (transaction == null || currentUser == null) {
            senderLabel.setText("Sender");
            empfaengerLabel.setText("Empfänger");
            betragLabel.setText("Betrag");
            verwendungszweckLabel.setText("Verwendungszweck");
            return;
        }

        String sender = buildPartyLabel(transaction.senderUsername(), "Sender unbekannt");
        String recipient = buildPartyLabel(transaction.recipientUsername(), "Empfänger unbekannt");
        String description = (transaction.description() == null || transaction.description().isBlank()) ? "Kein Verwendungszweck" : transaction.description();

        senderLabel.setText(sender);
        empfaengerLabel.setText(recipient);
        betragLabel.setText(transaction.amountStringUnsigned());
        verwendungszweckLabel.setText(description);
    }

    private String buildPartyLabel(String username, String unknownLabel) {
        if (username == null || username.isBlank()) {
            return unknownLabel;
        }
        if (currentUser != null && username.equals(currentUser.getUsername())) {
            return "Du";
        }
        return username;
    }

    private void updateClickability() {
        setClickableStyle(senderBox, onSenderClicked != null);
        setClickableStyle(empfaengerBox, onRecipientClicked != null);
        setClickableStyle(betragBox, onValueClicked != null);
        setClickableStyle(verwendungszweckBox, onDescriptionClicked != null);
    }

    private void setClickableStyle(StackPane pane, boolean clickable) {
        if (pane == null) {
            return;
        }
        pane.getStyleClass().removeAll("clickable-disabled", "clickable-enabled");
        pane.getStyleClass().add(clickable ? "clickable-enabled" : "clickable-disabled");
        pane.setMouseTransparent(!clickable);
    }

    private void handleAndClose(Object source, Consumer<TransactionLite> handler) {
        if (handler == null || transaction == null) {
            return;
        }
        handler.accept(transaction);
        closeStageFromSource(source);
    }

    private void closeStageFromSource(Object source) {
        if (!(source instanceof Node node)) {
            return;
        }
        Stage stage = (Stage) node.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}