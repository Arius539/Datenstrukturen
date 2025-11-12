package org.fpj.javafxController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.fpj.Data.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountPageController {

    private final TransactionController transactionController;

    @Autowired
    public AccountPageController(TransactionController transactionController){
        this.transactionController = transactionController;
    }

    @FXML
    private Label usernameLabel;
    @FXML
    private Label balanceLabel;

    @FXML
    private void toTransactionPage(ActionEvent event){
        Button button = (Button) event.getSource();
        transactionController.setMovementKind(getKind(button.getText()));

    }

    private TransactionType getKind(final String btnText){
        return switch (btnText) {
            case "Überweisung" -> TransactionType.UEBERWEISUNG;
            case "Einzahlung" -> TransactionType.EINZAHLUNG;
            case "Auszahlung" -> TransactionType.AUSZAHLUNG;
            default -> throw new IllegalArgumentException();
        };
    }
}
