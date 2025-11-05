package org.fpj;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

public class JavafxApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavafxApp.class);
    private ConfigurableApplicationContext springContext;

    @Override
    public void init(){
        springContext = new SpringApplicationBuilder(App.class).run();
    }

    @Override
    public void start(Stage stage){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Menu.fxml"));
        loader.setControllerFactory(springContext::getBean);
        try {
            Scene scene = new Scene(loader.load());
            stage.setTitle("Bezahl-Platform");
            stage.setScene(scene);
            stage.show();
        }
        catch (IOException e){
            LOGGER.error("Scene konnte nicht gelauncht werden.", e);
            stop();
        }
    }

    @Override
    public void stop(){
        springContext.close();
    }
}
