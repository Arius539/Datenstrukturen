package org.fpj.javafxController;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fpj.Data.UiHelpers;
import org.fpj.messaging.application.DirectMessageRow;
import org.fpj.messaging.application.DirectMessageService;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class ChatWindowController {
    @Autowired
    private DirectMessageService directMessageService;

    private final ObservableList<DirectMessage> chatMessages = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_CHAT_Messages = 20;
    private int currentPageChatMessages = 0;
    private boolean lastPageLoadedChatMessages = false;
    private boolean loadingNextPageChatMessages = false;

    private User currentUser;
    private User currentChatPartner;

    @FXML
    private Label lblContact;

    @FXML
    private Button btnExport;

    @FXML
    private VBox vbMessages;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextArea taInput;

    @FXML
    private Button btnSend;

    @FXML
    public void initialize() {

    }

    public void openChat(User currentUser, User chatPartner) {
        this.currentUser = currentUser;
        this.currentChatPartner = chatPartner;
        lblContact.setText(chatPartner.getUsername().equals(currentUser.getUsername())? "Du": chatPartner.getUsername());

        this.setupScrollPanel();
        this.scrollToBottom();

        initChatMessages();
    }

    private void initChatMessages() {
        currentPageChatMessages = 0;
        lastPageLoadedChatMessages = false;
        loadingNextPageChatMessages = false;

        chatMessages.clear();
        vbMessages.getChildren().clear();

        this.scrollToBottom();
        loadNextPageChatMessages();
    }

    // <editor-fold defaultstate="collapsed" desc="Infinite Scroll Messages">
    private void ensureNextChatMessagesPageLoaded() {
        if (loadingNextPageChatMessages || lastPageLoadedChatMessages) {
            return;
        }
        loadNextPageChatMessages();
    }

    private boolean canLoadNextChatMessagesPage() {
        return !loadingNextPageChatMessages && !lastPageLoadedChatMessages;
    }

    private void loadNextPageChatMessages() {
        if (!canLoadNextChatMessagesPage()) {
            return;
        }
        if (currentUser == null || currentChatPartner == null) {
            return;
        }

        loadingNextPageChatMessages = true;
        int pageToLoad = currentPageChatMessages;

        Task<Page<DirectMessage>> task = createChatMessagesPageTask(currentUser, currentChatPartner, pageToLoad);

        task.setOnSucceeded(ev -> onChatMessagesPageLoaded(task.getValue(), pageToLoad));
        task.setOnFailed(ev -> onChatMessagesPageFailed(task.getException(), pageToLoad));

       UiHelpers.startBackgroundTask(task, "chat-messages-loader-" + pageToLoad);
    }


    private Task<Page<DirectMessage>> createChatMessagesPageTask(User currentUser, User contact, int pageToLoad) {
        return new Task<>() {
            @Override
            protected Page<DirectMessage> call() {
                PageRequest pageRequest = PageRequest.of(pageToLoad, PAGE_SIZE_CHAT_Messages);
                return directMessageService.getConversation(currentUser, contact, pageRequest);
            }
        };
    }

    private void onChatMessagesPageLoaded(Page<DirectMessage> page, int pageToLoad) {
        try {
            List<DirectMessage> desc = page.getContent();
            int beforeCount = this.chatMessages.size();
            double oldV = this.scrollPane.getVvalue();   // aktueller Scrollzustand vor dem Einfügen

            for (DirectMessage msg : desc) {
                chatMessages.add(msg);
                addMessageNode(msg, true);
            }


            int afterCount = this.chatMessages.size();
            if (afterCount > 0 && beforeCount > 0) {
                double r = (double) beforeCount / (double) afterCount;
                double newV = r * oldV + (1.0 - r);     // gleiche Sichtposition relativ zum unteren Rand
                this.scrollPane.setVvalue(newV);
            }
            lastPageLoadedChatMessages = page.isLast();
            currentPageChatMessages = pageToLoad + 1;
        } finally {
            loadingNextPageChatMessages = false;
        }
    }
    // </editor-fold>

    private void onChatMessagesPageFailed(Throwable ex, int pageToLoad) {
        loadingNextPageChatMessages = false;
        showError("Chat-Nachrichten Seite " + pageToLoad +
                " konnte nicht geladen werden: " +
                (ex != null ? ex.getMessage() : "Unbekannter Fehler"));
    }

    private void addMessageNode(DirectMessage msg, boolean prepend) {
        boolean outgoing = isOutgoing(msg);

        HBox row = new HBox(8);
        row.getStyleClass().add("message-row");
        row.getStyleClass().add(outgoing ? "outgoing" : "incoming");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox bubble = new VBox(2);
        bubble.getStyleClass().add("message-bubble");
        bubble.getStyleClass().add(outgoing ? "outgoing" : "incoming");

        Label lblText = new Label(msg.getContent());
        lblText.setWrapText(true);
        lblText.setMaxWidth(440);
        lblText.getStyleClass().add("message-text");

        Label lblTimestamp = new Label(UiHelpers.formatInstant(msg.getCreatedAt()));
        lblTimestamp.getStyleClass().add("message-meta");

        bubble.getChildren().addAll(lblText, lblTimestamp);

        if (outgoing) {
            row.getChildren().addAll(spacer, bubble);
        } else {
            row.getChildren().addAll(bubble, spacer);
        }

        if (prepend) {
            vbMessages.getChildren().add(0, row);
        } else {
            vbMessages.getChildren().add(row);
        }
    }

    private boolean isOutgoing(DirectMessage msg) {
        if (currentUser == null || msg == null || msg.getSender() == null) {
            return false;
        }
        return currentUser.getId().equals(msg.getSender().getId());
    }

    private void scrollToBottom() {
        if (scrollPane == null) {
            return;
        }
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    public void setupScrollPanel(){
        if (scrollPane != null) {
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() <= 0.10) {
                    ensureNextChatMessagesPageLoaded();
                }
            });
        }
    }

    @FXML
    private void send() {
        String input = taInput.getText();
        if (input == null || input.isBlank()) {
            return;
        }
        DirectMessageRow row=  new DirectMessageRow(this.currentUser, this.currentChatPartner, input);
        DirectMessage directMessage = directMessageService.addDirectMessage(row);
        addMessageNode(directMessage, false);
        chatMessages.add(directMessage);
        taInput.clear();
        Platform.runLater(() -> vbMessages.layout());
    }

    @FXML
    private void exportChat() {

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
}
