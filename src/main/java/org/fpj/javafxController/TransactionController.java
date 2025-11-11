package org.fpj.javafxController;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import lombok.Setter;
import org.fpj.Data.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class TransactionController {

    @FXML private TextField recipientInput;
    @FXML private TextField amountInput;

    @Setter
    private TransactionType movementKind;

    @FXML
    private void submit(){

    }

}
