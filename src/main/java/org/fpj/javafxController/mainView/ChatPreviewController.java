package org.fpj.javafxController.mainView;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.Data.UiHelpers;
import org.fpj.javafxController.ChatWindowController;
import org.fpj.messaging.application.ChatPreview;
import org.fpj.messaging.application.DirectMessageService;
import org.fpj.payments.application.TransactionService;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class ChatPreviewController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private DirectMessageService directMessageService;

    private final ObservableList<ChatPreview> chatPreviews = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_CHAT_PREVIEWS = 50;
    private int currentPageChatPreviews = 0;
    private boolean lastPageLoadedChatPreviews = false;
    private boolean loadingNextPageChatPreviews = false;

    @FXML
    private TextField chatsUsernameSearch;

    @FXML private ListView<ChatPreview> lvChats;

    User currentUser;

    public void initialize(User currentUser) {
        this.currentUser = currentUser;
        initChatList();
        loadChatPreviewsFirstPage();

        setUpAutoCompletion();
    }

    private void addChatPreview(ChatPreview  chatPreview) {
        this.chatPreviews.add(0,chatPreview);
    }

    private void loadChatPreviewsFirstPage() {
        currentPageChatPreviews = 0;
        lastPageLoadedChatPreviews = false;
        chatPreviews.clear();
        loadNextPageChatPreview();
    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    private void initChatList() {
        lvChats.setItems(this.chatPreviews);

        lvChats.setCellFactory(list -> new ListCell<ChatPreview>() {
            private final Label title = new Label();
            private final Label subtitle = new Label();
            private final VBox left = new VBox(2, title, subtitle);
            private final Label ts = new Label();
            private final Region spacer = new Region();
            private final HBox root = new HBox(8, left, spacer, ts);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(ChatPreview item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                title.setText(item.name().equals(currentUser.getUsername())? "Du": item.name());

                String msg = item.lastMessage();
                if (msg == null || msg.isBlank()) {
                    subtitle.setText("Noch keine Nachrichten");
                } else {
                    String messageSender= item.lastMessageUsername().equals(currentUser.getUsername()) ? "Du: " : item.lastMessageUsername()+ ": ";
                    subtitle.setText(UiHelpers.truncate(messageSender, 20)+ UiHelpers.truncate(msg, 20));
                }

                ts.setText(item.timestamp() == null ? "" : TS.format(item.timestamp()));

                setGraphic(root);

                int index = getIndex();
                ensureNextChatPreviewPageLoaded(index);

                setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2) {
                        openChatForPreview(item);
                    }
                });
            }
        });
    }

    private void setUpAutoCompletion() {
        AutoCompletionBinding<String> binding =TextFields.bindAutoCompletion(chatsUsernameSearch, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) return List.of();
            return userService.usernameContaining(term);
        });

        binding.setOnAutoCompleted(event -> {
            String selected = event.getCompletion();
            chatsUsernameSearch.setText(selected);
            openChatForUsername(selected);
        });
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Infinite Scroll Chat Previews">
    private void ensureNextChatPreviewPageLoaded(int visibleIndex) {
        if (loadingNextPageChatPreviews || lastPageLoadedChatPreviews) {
            return;
        }

        int prefetchThreshold = 20;
        int size = chatPreviews.size();

        if (visibleIndex >= size - prefetchThreshold) {
            loadNextPageChatPreview();
        }
    }

    private void loadNextPageChatPreview() {
        if (!canLoadNextChatPreviewPage()) {
            return;
        }

        loadingNextPageChatPreviews = true;

        int pageToLoad = currentPageChatPreviews;

        Task<Page<ChatPreview>> task = createChatPreviewPageTask(pageToLoad);

        task.setOnSucceeded(ev -> onChatPreviewPageLoaded(task.getValue(), pageToLoad));
        task.setOnFailed(ev -> onChatPreviewPageFailed(task.getException(), pageToLoad));

        UiHelpers.startBackgroundTask(task, "chat-preview-loader-" + pageToLoad);
    }

    private boolean canLoadNextChatPreviewPage() {
        return !loadingNextPageChatPreviews && !lastPageLoadedChatPreviews;
    }

    private Task<Page<ChatPreview>> createChatPreviewPageTask(int pageToLoad) {
        return new Task<>() {
            @Override
            protected Page<ChatPreview> call() {
                var pageRequest = PageRequest.of(pageToLoad, PAGE_SIZE_CHAT_PREVIEWS);
                return directMessageService.getChatPreviews(
                        currentUser,
                        pageRequest
                );
            }
        };
    }
    private void onChatPreviewPageLoaded(Page<ChatPreview> page, int pageToLoad) {
        try {
            chatPreviews.addAll(page.getContent());
            lastPageLoadedChatPreviews = page.isLast();
            currentPageChatPreviews = pageToLoad + 1;
        } finally {
            loadingNextPageChatPreviews = false;
        }
    }

    private void onChatPreviewPageFailed(Throwable ex, int pageToLoad) {
        loadingNextPageChatPreviews = false;

        showError("Chat-Übersicht Seite " + pageToLoad +
                " konnte nicht geladen werden: " +
                (ex != null ? ex.getMessage() : "Unbekannter Fehler"));
    }
    // </editor-fold>

    private void openChatForPreview(ChatPreview preview) {
        if (preview == null) {
            return;
        }

        String username = preview.name();

        openChatForUsername(username);
    }

    private void openChatForUsername(String username) {

        if (username == null || username.isBlank()) {
            this.error("Kein Benutzername für den Chat ausgewählt.");
            return;
        }


        Optional<User> optUser = userService.findByUsername(username);

        if (optUser.isEmpty()) {
            error("Benutzer für Chat nicht gefunden: " + username);
            return;
        }
        try {
            ChatWindowController controller= loadChatWindow(username);
            User chatPartner = optUser.get();
            controller.openChat(currentUser, chatPartner);
        } catch (Exception e){
            error("Fehler beim laden des Chats aufgetreten. Versuche es bitte erneut.");
        }
    }

    private ChatWindowController loadChatWindow(String username) throws IOException {
        var url = getClass().getResource("/fxml/chat_window.fxml");
        if (url == null) {
            throw new IllegalStateException("chat_window.fxml nicht gefunden!");
        }

        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(applicationContext::getBean);

        Parent root = loader.load();
        ChatWindowController controller = loader.getController();

        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Chat mit " + username);
        stage.setScene(new javafx.scene.Scene(root));
        stage.show();
        return   controller;
    }

    @FXML
    private void onReloadChats() {
        this.loadingNextPageChatPreviews = false;
        this.lastPageLoadedChatPreviews = false;
        this.currentPageChatPreviews = 0;
        loadChatPreviewsFirstPage();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void info(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    private void error(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        a.setHeaderText("Fehler");
        a.setTitle("Fehler");
        a.showAndWait();
    }
}
