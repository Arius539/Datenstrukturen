package org.fpj.javafxController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.payments.domain.TransactionRow;
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

    @FXML
    private Button reuseButton;

    private TransactionLite transaction;

    private Consumer<TransactionLite> onSenderClicked;

    private Consumer<TransactionLite> onRecipientClicked;

    private Consumer<TransactionLite> onReuseClicked;

    private Consumer<TransactionLite> onDescriptionClicked;

    private Consumer<TransactionLite> onValueClicked;

    private User currentUser;


    public void initialize(TransactionLite transaction, User currentUser, Consumer<TransactionLite> onSenderClicked, Consumer<TransactionLite> onRecipientClicked, Consumer<TransactionLite> onReuseClicked, Consumer<TransactionLite> onDescriptionClicked, Consumer<TransactionLite> onValueClicked ) {
        this.currentUser = currentUser;
        this.transaction = transaction;
        this.onSenderClicked = onSenderClicked;
        this.onReuseClicked = onReuseClicked;
        this.onDescriptionClicked = onDescriptionClicked;
        this.onValueClicked = onValueClicked;
        this.onRecipientClicked = onRecipientClicked;
        updateClickability();
        updateView();
    }

    private void updateView() {
        if (transaction == null) {
            senderLabel.setText("Sender");
            empfaengerLabel.setText("Empfänger");
            betragLabel.setText("Betrag");
            verwendungszweckLabel.setText("Verwendungszweck");
            return;
        }

        String sender= (transaction.senderUsername()==null || transaction.senderUsername().isEmpty())? "Sender Unbekannt": transaction.senderUsername().equals(currentUser.getUsername())? "Du":transaction.senderUsername();
        String recipient= (transaction.recipientUsername()==null || transaction.recipientUsername().isEmpty())? "Empfänger Unbekannt": transaction.recipientUsername().equals(currentUser.getUsername())? "Du": transaction.recipientUsername();
        String description = (transaction.description() == null || transaction.description().isBlank()) ? "Keine Verwendungszweck" : transaction.description();
        senderLabel.setText(sender);
        empfaengerLabel.setText(recipient);
        betragLabel.setText(transaction.amountStringUnsigned());
        verwendungszweckLabel.setText(description);
    }

    private void updateClickability() {
        boolean clickable = (onSenderClicked!= null);
        setClickableStyle(senderBox, clickable);
        clickable = (onRecipientClicked!= null);
        setClickableStyle(empfaengerBox, clickable);
        clickable = (onValueClicked!= null);
        setClickableStyle(betragBox, clickable);
        clickable = (onDescriptionClicked!= null);
        setClickableStyle(verwendungszweckBox, clickable);
    }

    private void setClickableStyle(StackPane pane, boolean clickable) {
        if (pane == null) {
            return;
        }
        pane.getStyleClass().removeAll("clickable-disabled", "clickable-enabled");
        pane.getStyleClass().add(clickable ? "clickable-enabled" : "clickable-disabled");
        pane.setMouseTransparent(!clickable);
    }

    @FXML
    private void onSenderClicked(MouseEvent event) {
        if (onSenderClicked != null && transaction != null) {
        }
    }

    @FXML
    private void onRecipientClicked(MouseEvent event) {
        if (onRecipientClicked != null && transaction != null) {
        }
    }

    @FXML
    private void onValueClicked(MouseEvent event) {
        if (onValueClicked != null && transaction != null) {
        }
    }

    @FXML
    private void onDescriptionClicked(MouseEvent event) {
        if (onDescriptionClicked != null && transaction != null) {
        }
    }

    @FXML
    private void onReuseClicked(ActionEvent event) {
        if (onReuseClicked != null && transaction != null) {
            this.onReuseClicked.accept(transaction);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        }
    }
}

