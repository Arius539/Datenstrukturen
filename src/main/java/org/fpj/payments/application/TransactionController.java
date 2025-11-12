package org.fpj.payments.application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import lombok.Getter;
import lombok.Setter;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class TransactionController {
    @Autowired
    private TransactionService transactionService;


    private final ObservableList<TransactionItemLite> liteTransactionList = FXCollections.observableArrayList();
    private static final int PAGE_SIZE_Lite_List = 100;
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

        int prefetchThreshold = 100;
        int size = liteTransactionList.size();

        if (visibleIndex >= size - prefetchThreshold) {
            loadNextPageLite(user);
        }
    }

    /** Task to Load LiteTransactions */
    private void loadNextPageLite(User user) {
        if (!canLoadNextLitePage()) {
            return;
        }

        loadingNextPageLiteList = true;

        int pageToLoad = currentPageLiteList;
        long userId = user.getId();

        Task<Page<TransactionItemLite>> task = createLitePageTask(userId, pageToLoad);

        task.setOnSucceeded(ev -> onLitePageLoaded(task.getValue(), pageToLoad));
        task.setOnFailed(ev -> onLitePageFailed(task.getException()));

        startBackgroundTask(task, pageToLoad);
    }


    private boolean canLoadNextLitePage() {
        return !loadingNextPageLiteList && !lastPageLoadedLiteList;
    }

    private Task<Page<TransactionItemLite>> createLitePageTask(long userId, int pageToLoad) {
        return new Task<>() {
            @Override
            protected Page<TransactionItemLite> call() {
                return transactionService.findLiteItemsForUser(
                        userId,
                        pageToLoad,
                        PAGE_SIZE_Lite_List
                );
            }
        };
    }

    private void onLitePageLoaded(Page<TransactionItemLite> page, int pageToLoad) {
        try {
            liteTransactionList.addAll(page.getContent());
            lastPageLoadedLiteList = page.isLast();
            currentPageLiteList = pageToLoad + 1;
        } finally {
            loadingNextPageLiteList = false;
        }
    }

    private void onLitePageFailed(Throwable ex) {
        loadingNextPageLiteList = false;

        showError("Transaktionen konnten nicht geladen werden: " +
                (ex != null ? ex.getMessage() : "Unbekannter Fehler"));
    }

    private void startBackgroundTask(Task<?> task, int pageToLoad) {
        Thread t = new Thread(task, "trx-page-loader-" + pageToLoad);
        t.setDaemon(true);
        t.start();
    }

    private void showError(String message) {
        // falls du sicher im FX-Thread bist, kannst du Platform.runLater weglassen
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
