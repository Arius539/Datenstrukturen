package org.fpj.messaging.application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import org.fpj.payments.application.TransactionItemLite;
import org.fpj.payments.application.TransactionService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class DirectMessageController {
    @Autowired
    private DirectMessageService directMessageService;

    private final ObservableList<ChatPreview> chatPreviews = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_CHAT_PREVIEWS = 5;
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

        int prefetchThreshold = 5;
        int size = chatPreviews.size();

        if (visibleIndex >= size - prefetchThreshold) {
            loadNextPageChatPreview(user);
        }
    }

    private void loadNextPageChatPreview(User user) {
        if (loadingNextPageChatPreviews || lastPageLoadedChatPreviews) {
            return;
        }

        loadingNextPageChatPreviews = true;
        var pageRequest = PageRequest.of(currentPageChatPreviews, PAGE_SIZE_CHAT_PREVIEWS);
        var page = directMessageService.getChatPreviews(
                user,
                pageRequest
        );

        chatPreviews.addAll(page.getContent());

        lastPageLoadedChatPreviews = page.isLast();
        currentPageChatPreviews++;

        loadingNextPageChatPreviews = false;
    }
}
