package org.fpj.javafxController.mainView;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.fpj.Data.UiHelpers;
import org.fpj.javafxController.ChatWindowController;
import org.fpj.messaging.application.ChatPreview;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.application.TransactionItemLite;
import org.fpj.payments.application.TransactionResult;
import org.fpj.Exceptions.TransactionException;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.TextFields;


import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import static org.fpj.Data.UiHelpers.parseAmountTolerant;
import static org.fpj.Data.UiHelpers.safe;

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
    @Autowired
    private TransactionService transactionService;

    // Profil/Saldo
    @FXML private Label lblEmail;
    @FXML private Label lblBalance;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private User currentUser;

    @FXML
    public void initialize() {
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
