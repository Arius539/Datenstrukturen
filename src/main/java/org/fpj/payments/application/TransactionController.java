package org.fpj.payments.application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class TransactionController {
    @Autowired
    private TransactionService transactionService;


    private final ObservableList<TransactionItemLite> liteTransactionList = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_Lite_List = 5;
    private int currentPageLiteList = 0;
    private boolean lastPageLoadedLiteList = false;
    private boolean loadingNextPageLiteList = false;

    public void addLiteTransaction(TransactionItemLite  transactionItem) {
        this.liteTransactionList.add(0,transactionItem);
    }

    public void loadTransactionsFirstPageLite(User user) {
        currentPageLiteList = 0;
        lastPageLoadedLiteList = false;
        liteTransactionList.clear();
        loadNextPageLite(user);
    }

    public void ensureNextPageLoaded(int visibleIndex, User user) {
        if (loadingNextPageLiteList || lastPageLoadedLiteList) {
            return;
        }

        int prefetchThreshold = 50;
        int size = liteTransactionList.size();

        if (visibleIndex >= size - prefetchThreshold) {
            loadNextPageLite(user);
        }
    }

    private void loadNextPageLite(User user) {
        if (loadingNextPageLiteList || lastPageLoadedLiteList) {
            return;
        }

        loadingNextPageLiteList = true;

        var page = transactionService.findLiteItemsForUser(
                user.getId(),
                currentPageLiteList,
                PAGE_SIZE_Lite_List
        );

        liteTransactionList.addAll(page.getContent());

        lastPageLoadedLiteList = page.isLast();
        currentPageLiteList++;

        loadingNextPageLiteList = false;
    }
}
