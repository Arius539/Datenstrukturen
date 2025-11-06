package org.fpj.javafxController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainController {

    @FXML
    private StackPane root;
    @FXML
    private AnchorPane overlayContainer;

    private final ApplicationContext springContext;

    @Autowired
    public MainController(ApplicationContext springContext){
        this.springContext = springContext;
    }

    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(springContext::getBean);
        VBox loginPane = loader.load();

        LoginController loginController = loader.getController();
        loginController.setMainController(this);

        AnchorPane.setTopAnchor(loginPane, (overlayContainer.getHeight() - loginPane.getPrefHeight()) / 2);
        AnchorPane.setLeftAnchor(loginPane, (overlayContainer.getWidth() - loginPane.getPrefWidth()) / 2);

        overlayContainer.getChildren().setAll(loginPane);

        overlayContainer.widthProperty().addListener((observable, oldValue, newValue) ->
                AnchorPane.setLeftAnchor(loginPane, (newValue.doubleValue() - loginPane.getPrefWidth()) / 2));
        overlayContainer.heightProperty().addListener((observable, oldValue, newValue) ->
                AnchorPane.setTopAnchor(loginPane, (newValue.doubleValue() - loginPane.getPrefHeight()) / 2));
    }

    public void hideLogin(){
        overlayContainer.getChildren().clear();
        root.getChildren().remove(overlayContainer);
    }

}