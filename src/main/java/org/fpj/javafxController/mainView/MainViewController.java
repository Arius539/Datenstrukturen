package org.fpj.javafxController.mainView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.fpj.AlertService;
import org.fpj.Data.WindowInformationResponse;
import org.fpj.ViewNavigator;
import org.fpj.javafxController.TransactionViewController;
import org.fpj.javafxController.WallCommentViewController;
import org.fpj.payments.application.TransactionService;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MainViewController {

    private final ApplicationContext applicationContext;
    private final TransactionsLiteViewController transactionsLiteController;
    private final ChatPreviewController chatPreviewController;
    private final AlertService alertService;
    private final ViewNavigator viewNavigator;

    @FXML
    private Label lblEmail;

    @FXML
    private Label lblBalance;

    private User currentUser;

    @Autowired
    public MainViewController(ViewNavigator viewNavigator,ApplicationContext context, TransactionsLiteViewController transactionsLiteController, ChatPreviewController chatPreviewController, AlertService alertService) {
        this.viewNavigator = viewNavigator;
        this.applicationContext = context;
        this.transactionsLiteController = transactionsLiteController;
        this.chatPreviewController = chatPreviewController;
        this.alertService = alertService;
    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    @FXML
    public void initialize() {
        try {
            currentUser = applicationContext.getBean("loggedInUser", User.class);
        } catch (Exception e) {
            alertService.error("Fehler", "Benutzer konnte nicht geladen werden", "Der angemeldete Benutzer konnte nicht aus dem Kontext geladen werden.");
            return;
        }

        if (currentUser == null) {
            alertService.error("Fehler", "Benutzer fehlt", "Es ist kein angemeldeter Benutzer vorhanden.");
            return;
        }

        lblEmail.setText(currentUser.getUsername());
        transactionsLiteController.initialize(currentUser, this::updateBalanceLabel);
        chatPreviewController.initialize(currentUser);
    }
    // </editor-fold>

    @FXML
    public void actionTransactions() {
        try{
            WindowInformationResponse<TransactionViewController> response= viewNavigator.loadTransactionView();
            if(!response.isLoaded()) response.controller().initialize(currentUser, null);
        }catch (Exception e){
            this.alertService.error("Fehler", "Fehler", "Es ist eine Fehler beim Laden des Transaktionsfensters aufgetreten");
        }

    }

    @FXML
    public void actionWallComments() {
        try{
           WindowInformationResponse<WallCommentViewController> response= viewNavigator.loadWallCommentView();
           if(!response.isLoaded()) response.controller().load(currentUser, currentUser);;
        }catch (Exception e){
            this.alertService.error("Fehler", "Fehler", "Es ist eine Fehler beim Laden des Transaktionsfensters aufgetreten");
        }
    }

    private void updateBalanceLabel(String balance) {
        lblBalance.setText(balance);
    }
}
