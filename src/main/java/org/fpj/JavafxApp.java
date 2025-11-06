package org.fpj;

import javafx.application.Application;
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
        springContext.getBeanFactory().registerSingleton("stage", stage);

        ViewNavigator viewNavigator = springContext.getBean(ViewNavigator.class);
        viewNavigator.setStage(stage);
        try {
            viewNavigator.showMain();
        }
        catch (IOException e){
            LOGGER.error("App konnte nicht gestartet werden.", e);
            System.exit(0);
        }
    }

    @Override
    public void stop(){
        springContext.close();
    }
}
