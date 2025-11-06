package org.fpj.javafxController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.fpj.Data.MoneyMovement;
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

    private MoneyMovement getKind(final String btnText){
        return switch (btnText) {
            case "Überweisung" -> MoneyMovement.TRANSACTION;
            case "Einzahlung" -> MoneyMovement.PAY_IN;
            case "Auszahlung" -> MoneyMovement.PAY_OUT;
            default -> throw new IllegalArgumentException();
        };
    }
}
