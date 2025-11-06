package org.fpj.javafxController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainController {

    @FXML
    private StackPane root;
    @FXML
    private AnchorPane overlayContainer;

    private final LoginController loginController;

    @Autowired
    public MainController(LoginController loginController){
        this.loginController = loginController;
    }

    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        VBox loginPane = loader.load();

        AnchorPane.setTopAnchor(loginPane, (overlayContainer.getHeight() - loginPane.getPrefHeight()) / 2);
        AnchorPane.setLeftAnchor(loginPane, (overlayContainer.getWidth() - loginPane.getPrefWidth()) / 2);

        loginController.setMainController(this);
        overlayContainer.getChildren().setAll(loginPane);

        overlayContainer.widthProperty().addListener((observable, oldValue, newValue) ->
                AnchorPane.setLeftAnchor(loginPane, (newValue.doubleValue() - loginPane.getPrefWidth()) / 2));
        overlayContainer.heightProperty().addListener((observable, oldValue, newValue) ->
                AnchorPane.setTopAnchor(loginPane, (newValue.doubleValue() - loginPane.getPrefHeight()) / 2));
    }

    public void hideLogin(){
        overlayContainer.getChildren().clear();
    }

}