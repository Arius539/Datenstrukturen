package org.fpj;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fpj.Data.WindowInformation;
import org.fpj.Data.WindowInformationResponse;
import org.fpj.javafxController.TransactionViewController;
import org.fpj.javafxController.WallCommentViewController;
import org.fpj.javafxController.ChatWindowController;
import org.fpj.javafxController.TransactionDetailController;
import org.fpj.javafxController.CsvImportDialogController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ViewNavigator {

    private final ApplicationContext context;
    private final Map<String, WindowInformation> openWindows = new HashMap<>();

    @Autowired
    public ViewNavigator(ApplicationContext context) {
        this.context = context;
    }

    private <T> WindowInformationResponse loadView(String key, String fxml, String title, double width, double height, boolean alwaysOnTop, Class<T> controllerType) throws IOException {
        WindowInformation existing = openWindows.get(key);
        if (existing != null) {
            Stage stage = existing.windowStage();
            if (stage.isShowing()) {
                stage.toFront(); stage.requestFocus();
                if (controllerType == null) {
                    return new WindowInformationResponse(null, true);
                }
                return new WindowInformationResponse<T>(controllerType.cast(existing.controller()),true);
            } else {
                openWindows.remove(key);
            }
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.setAlwaysOnTop(alwaysOnTop);
        stage.setIconified(false);
        stage.show();
        stage.toFront();
        stage.requestFocus();

        Object controller = loader.getController();
        WindowInformation info = new WindowInformation(stage, controller);
        openWindows.put(key, info);
        stage.setOnHidden(e -> openWindows.remove(key));

        if (controllerType == null) {
            return new WindowInformationResponse<T>(null, false);
        }
        if (!controllerType.isInstance(controller)) {
            throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ " + controllerType.getName() + ", sondern " + controller.getClass().getName());
        }
        return new WindowInformationResponse<T>(controllerType.cast(controller), false);
    }

    public void loadMain() throws IOException {
        loadView("main", "mainView/main_view.fxml", "PayTalk", 1280, 860, false, null);
    }

    public void loadLogin() throws IOException {
        loadView("login", "login.fxml", "PayTalk: Login", 400, 400, true, null);
    }

    public void closeLogin() {
        WindowInformation info = openWindows.get("login");
        if (info == null) { return; }
        Stage stage = info.windowStage();
        if (stage != null && stage.isShowing()) { stage.close(); } else { openWindows.remove("login"); }
    }

    public WindowInformationResponse<TransactionViewController> loadTransactionView() throws IOException {
        return loadView("transactionView", "transactionView.fxml", "PayTalk: Transaktionsübersicht", 1280, 860, false, TransactionViewController.class);
    }

    public WindowInformationResponse<WallCommentViewController> loadWallCommentView() throws IOException {
        return loadView("wallCommentView", "wallCommentView.fxml", "PayTalk: Pinnwand", 1280, 860, false, WallCommentViewController.class);
    }

    public WindowInformationResponse<ChatWindowController> loadChatView(String chatPartner) throws IOException {
        return loadView("chat:" + chatPartner, "chat_window.fxml", "PayTalk: Chat mit: " + chatPartner, 800, 600, false, ChatWindowController.class);
    }

    public WindowInformationResponse<TransactionDetailController> loadTransactionDetailView() throws IOException {
        return loadView("transactionDetail", "transaction_detail.fxml", "PayTalk: Transaktionsinfos", 600, 300, false, TransactionDetailController.class);
    }

    public WindowInformationResponse<CsvImportDialogController> loadCsvDialogView() throws IOException {
        return loadView("csvImport", "csvImportDialog.fxml", "PayTalk: Csv Importer", 800, 400, false, CsvImportDialogController.class);
    }
}
