package org.fpj.javafxController.mainView;

import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    public MainViewController(ApplicationContext context, TransactionsLiteViewController transactionsLiteController, ChatPreviewController chatPreviewController,
                              UserService userService, TransactionService transactionService){
        this.applicationContext = context;
        this.transactionsLiteController = transactionsLiteController;
        this.chatPreviewController = chatPreviewController;
        this.userService = userService;
        this.transactionService = transactionService;
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

        currentUser = userService.currentUser();
        lblEmail.setText(currentUser.getUsername());

        transactionsLiteController.initialize(currentUser, this::updateBalanceLabel);
        chatPreviewController.initialize(currentUser);
    }

    @FXML public void actionTransactions()    { }
    @FXML public void actionWallComments()    { info("Navigation: Wall Kommentare (Placeholder)."); }
    @FXML public void actionDirectMessages()  { info("Navigation: Massen Transaktion (Placeholder)."); }

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
