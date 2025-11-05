package org.fpj;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ViewNavigator {

    private final ApplicationContext context;
    @Setter
    private Stage stage;

    @Autowired
    public ViewNavigator(ApplicationContext context){
        this.context = context;
    }

    public void showMenu() throws Exception {
        loadView("Menu.fxml", "Menu");
    }

    public void showAccount() throws Exception {
        loadView("account.fxml", "Konto");
    }

    private void loadView(String fxml, String title) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.setTitle(title);
        stage.show();
    }

}
