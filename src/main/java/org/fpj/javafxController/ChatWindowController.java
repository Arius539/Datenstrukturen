package org.fpj.javafxController;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype") // wichtig: pro Fenster ein eigener Controller
public class ChatWindowController {

    @FXML private Label lblContact;
    @FXML private ListView<String> lvMessages;
    @FXML private TextField tfInput;

    private String contact;

    /**
     * Wird unmittelbar nach dem Laden (in MainViewController) aufgerufen.
     */
    public void init(String contact) {
        this.contact = contact;
        lblContact.setText(contact);
        lvMessages.setItems(FXCollections.observableArrayList(
                contact + ": Hi!",
                "Ich: Hallo " + contact + ", alles gut?",
                contact + ": Ja, danke."
        ));
    }

    @FXML
    public void send() {
        String msg = tfInput.getText();
        if (msg == null || msg.isBlank()) return;
        lvMessages.getItems().add("Ich: " + msg);
        tfInput.clear();
    }
}
