package org.fpj.javafxController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.fpj.Exceptions.LoginFailedException;
import org.fpj.ViewNavigator;
import org.fpj.users.application.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginController {

    private static final String REGISTER_STRING = "registrieren";
    private static final String LOGIN_STRING = "anmelden";
    private static final String NO_ACCOUNT = "noch kein Konto?";
    private static final String ACCOUNT_EXISTENT = "du hast bereits ein Konto?";

    private final GenericApplicationContext context;
    private final ViewNavigator viewNavigator;
    private final LoginService loginService;

    @Autowired
    public LoginController(GenericApplicationContext context, ViewNavigator viewNavigator, LoginService loginService){
        this.context = context;
        this.viewNavigator = viewNavigator;
        this.loginService = loginService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

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

    @FXML
    private void submit(ActionEvent event){
        Button button = (Button) event.getSource();
        final String username = usernameInput.getText();
        final String password = passwordInput.getText();
        if (button.getText().equals(LOGIN_STRING)){
            doLogin(username, password);
        }
        else {
            doRegister(username, password);
        }
    }

    private void doRegister(String username, String password) {
        final String check = passwordCheck.getText();
        try {
            loginService.register(username, password, check);

            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText("Registrierung erfolgreich");
            alert.setContentText("Für Benutzer " + username + " wurde erfolgreich ein Account erstellt. Bitte melde dich im nächsten Schritt an.");
            alert.showAndWait();

            toggleLoginAndRegister();
        }
        catch (LoginFailedException e){
            final Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Info");
            alert.setHeaderText("Login fehlgeschlagen");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void doLogin(String username, String password) {
        try {
            loginService.login(username, password);

            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText("Login erfolgreich");
            alert.setContentText("Mit Benutzer " + username + " eingelogged.");
            alert.showAndWait();

            viewNavigator.loadMain();
        }
        catch (LoginFailedException e){
            final Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Info");
            alert.setHeaderText("Login fehlgeschlagen");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
        catch (IOException e){
            LOGGER.error("Fenster konnte nicht geladen werden", e);
            System.exit(0);
        }
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
