package org.fpj.messaging.application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import lombok.Getter;
import lombok.Setter;
import org.fpj.payments.application.TransactionItemLite;
import org.fpj.payments.application.TransactionService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class DirectMessageController {
    @Autowired
    private DirectMessageService directMessageService;

    private final ObservableList<ChatPreview> chatPreviews = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_CHAT_PREVIEWS = 50;
    private int currentPageChatPreviews = 0;
    private boolean lastPageLoadedChatPreviews = false;
    private boolean loadingNextPageChatPreviews = false;

    public void addLiteTransaction(ChatPreview  chatPreview) {
        this.chatPreviews.add(0,chatPreview);
    }

    public void loadChatPreviewsFirstPage(User user) {
        currentPageChatPreviews = 0;
        lastPageLoadedChatPreviews = false;
        chatPreviews.clear();
        loadNextPageChatPreview(user);
    }

    public void ensureNextChatPreviewPageLoaded(int visibleIndex, User user) {
        if (loadingNextPageChatPreviews || lastPageLoadedChatPreviews) {
            return;
        }

        int prefetchThreshold = 20;
        int size = chatPreviews.size();

        if (visibleIndex >= size - prefetchThreshold) {
            loadNextPageChatPreview(user);
        }
    }

    private void loadNextPageChatPreview(User user) {
        if (!canLoadNextChatPreviewPage()) {
            return;
        }

        loadingNextPageChatPreviews = true;

        int pageToLoad = currentPageChatPreviews;

        Task<Page<ChatPreview>> task = createChatPreviewPageTask(user, pageToLoad);

        task.setOnSucceeded(ev -> onChatPreviewPageLoaded(task.getValue(), pageToLoad));
        task.setOnFailed(ev -> onChatPreviewPageFailed(task.getException(), pageToLoad));

        startBackgroundTask(task, "chat-preview-loader-" + pageToLoad);
    }

// ---------------- ChatPreviews: Hilfsfunktionen ----------------

    private boolean canLoadNextChatPreviewPage() {
        return !loadingNextPageChatPreviews && !lastPageLoadedChatPreviews;
    }

    private Task<Page<ChatPreview>> createChatPreviewPageTask(User user, int pageToLoad) {
        return new Task<>() {
            @Override
            protected Page<ChatPreview> call() {
                var pageRequest = PageRequest.of(pageToLoad, PAGE_SIZE_CHAT_PREVIEWS);
                return directMessageService.getChatPreviews(
                        user,
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

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread t = new Thread(task, threadName);
        t.setDaemon(true);
        t.start();
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
