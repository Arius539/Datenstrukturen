package org.fpj;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Setter;
import org.fpj.javafxController.MainController;
import org.fpj.javafxController.mainView.MainViewController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ViewNavigator {

    private final ApplicationContext context;
    private final MainViewController mainViewController;
    @Setter
    private Stage stage;

    @Autowired
    public ViewNavigator(ApplicationContext context, MainViewController mainViewController){
        this.context = context;
        this.mainViewController = mainViewController;
    }

    private void loadView(String fxml, String title, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.show();
    }

    public void loadMain() throws IOException{
        mainViewController.initialize();
        loadView("/mainView/main_view.fxml", "Bezahlplatform", 1280, 860);
    }

    public void loadLogin() throws IOException{
        loadView("/login.fxml", "Login", 600, 400);
    }

}
