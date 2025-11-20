package org.fpj.Data;

import ch.qos.logback.core.util.Loader;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public record WindowInformation<T>(Stage windowStage, T controller) {

}
