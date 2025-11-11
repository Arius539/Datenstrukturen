package org.fpj;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Setter;
import org.fpj.javafxController.MainController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ViewNavigator {

    private final ApplicationContext context;
    private final MainController mainController;
    @Setter
    private Stage stage;

    @Autowired
    public ViewNavigator(ApplicationContext context, MainController mainController){
        this.context = context;
        this.mainController = mainController;
    }

    private void loadView(String fxml, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.setTitle(title);
        stage.show();
    }

}
