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

}