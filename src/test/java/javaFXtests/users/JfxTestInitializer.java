package javaFXtests.users;

import javafx.application.Platform;

public class JfxTestInitializer {

    private static boolean initialized = false;

    public static void init(){
        if (!initialized){
            Platform.startup(() -> {});
            initialized = true;
        }
    }
}
