package org.fpj.javafxController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private static final String REGISTER_STRING = "registrieren";
    private static final String LOGIN_STRING = "anmelden";
    private static final String NO_ACCOUNT = "noch kein Konto?";
    private static final String ACCOUNT_EXISTENT = "du hast bereits ein Konto?";

    @FXML
    private TextField usernameInput;
    @FXML
    private PasswordField passwordInput;
    @FXML
    private PasswordField passwordCheck;
    @FXML
    private Button loginButton;
    @FXML
    private Button toggleButton;

    @Setter
    private MainController mainController;

    @FXML
    private void submit(ActionEvent event){
        Button button = (Button) event.getSource();
        final String username = usernameInput.getText();
        final String password = passwordInput.getText();
        if (button.getText().equals(LOGIN_STRING)){
            //Funktion aufrufen
        }
        else {
            final String check = passwordCheck.getText();
            //Funktion aufrufen
        }
        mainController.hideLogin();
    }

    @FXML
    private void toggleLoginAndRegister(){
        if (loginButton.getText().equals(LOGIN_STRING)){
            loginButton.setText(REGISTER_STRING);
            toggleButton.setText(ACCOUNT_EXISTENT);
            passwordCheck.setVisible(true);
        } else {
            loginButton.setText(LOGIN_STRING);
            toggleButton.setText(NO_ACCOUNT);
            passwordCheck.setVisible(false);
        }
    }
}
