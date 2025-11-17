package org.fpj.javafxController.mainView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.fpj.AlertService;
import javafx.stage.Stage;
import org.fpj.payments.application.TransactionService;
import org.fpj.users.domain.User;
import org.fpj.users.application.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


import java.time.format.DateTimeFormatter;

@Component
public class MainViewController {

    private final ApplicationContext applicationContext;

    private final TransactionsLiteViewController transactionsLiteController;

    private final ChatPreviewController chatPreviewController;

    private final UserService userService;

    private final TransactionService transactionService;
    private final AlertService alertService;

    @Autowired
    public MainViewController(ApplicationContext context, TransactionsLiteViewController transactionsLiteController, ChatPreviewController chatPreviewController,
                              UserService userService, TransactionService transactionService, AlertService alertService){
        this.applicationContext = context;
        this.transactionsLiteController = transactionsLiteController;
        this.chatPreviewController = chatPreviewController;
        this.userService = userService;
        this.transactionService = transactionService;
        this.alertService = alertService;
    }

    // Profil/Saldo
    @FXML private Label lblEmail;
    @FXML private Label lblBalance;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private User currentUser;

    @FXML
    public void initialize() {
        //TODO: Produktiv sollte folgendens ausgeführt werden:
        currentUser = applicationContext.getBean("loggedInUser", User.class);

        //folgendes nur vorübergehend
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

    @FXML public void actionTransactions()    { }
    @FXML public void actionWallComments()    {alertService.info("Info", "Info",
            "Navigation: Wall Kommentare (Placeholder).");}
    @FXML public void actionDirectMessages()  { alertService.error("Fehler", "Fehler",
            "Navigation: Massen Transaktion (Placeholder).");}

    private void updateBalanceLabel(String balance) {
        lblBalance.setText(balance);
    }

}
