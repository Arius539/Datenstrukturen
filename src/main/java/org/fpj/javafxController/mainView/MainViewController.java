package org.fpj.javafxController.mainView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.fpj.javafxController.TransactionViewController;
import org.fpj.payments.domain.TransactionViewSearchParameter;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


import java.time.format.DateTimeFormatter;

@Component
public class MainViewController {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TransactionsLiteViewController transactionsLiteController;

    @Autowired
    private ChatPreviewController chatPreviewController;

    @Autowired
    private UserService userService;

    @FXML private Label lblEmail;
    @FXML private Label lblBalance;

    private User currentUser;

    @FXML
    public void initialize() {
        if(!loadCurrentUser()) return;
        lblEmail.setText(currentUser.getUsername());

        transactionsLiteController.initialize(currentUser, this::updateBalanceLabel);
        chatPreviewController.initialize(currentUser);

    }

    public boolean loadCurrentUser(){
        try {
            currentUser = userService.currentUser();
        }catch(Exception e) {
            this.error("Wir konnten deine Benuterdaten nicht laden, bitte starte die Anwendung neu");
            Platform.runLater(() -> {
                Stage stage = (Stage) lblEmail.getScene().getWindow();
                stage.close();
            });
            return false;
        }
        return true;
    }
    public void openTransactionsWindow(TransactionViewSearchParameter transactionViewSearchParameter){
        try {
            var url = getClass().getResource("/fxml/transactionView.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            TransactionViewController detailController = loader.getController();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Transaktionen");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
            detailController.initialize(currentUser, transactionViewSearchParameter);
        } catch(Exception e) {
            error("Fehler beim laden des Transaktionsfensters. Versuche es erneut oder starte die Anwendung neu: ");
        }
    }

    @FXML public void actionTransactions()    {
        openTransactionsWindow(null);
    }
    @FXML public void actionWallComments()    { info("Navigation: Wall Kommentare (Placeholder)."); }

    private void updateBalanceLabel(String balance) {
        lblBalance.setText(balance);
    }

    private void info(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    private void error(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        a.setHeaderText("Fehler");
        a.setTitle("Fehler");
        a.showAndWait();
    }
}
