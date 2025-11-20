package org.fpj.paging;

import javafx.concurrent.Task;
import org.fpj.util.UiHelpers;
import org.springframework.data.domain.Page;
import java.util.function.Consumer;

public class InfinitePager<T> {

    @FunctionalInterface
    public interface PageFetcher<T> {
        Page<T> fetch(int pageIndex, int pageSize) throws Exception;
    }
    private final int pageSize;
    private final PageFetcher<T> pageFetcher;
    private final Consumer<Page<T>> pageConsumer;
    private final Consumer<Throwable> errorHandler;
    private final String taskNamePrefix;

    private int currentPage = 0;
    private boolean lastPageLoaded = false;
    private boolean loading = false;

    public InfinitePager(int pageSize,
                         PageFetcher<T> pageFetcher,
                         Consumer<Page<T>> pageConsumer,
                         Consumer<Throwable> errorHandler,
                         String taskNamePrefix) {
        this.pageSize = pageSize;
        this.pageFetcher = pageFetcher;
        this.pageConsumer = pageConsumer;
        this.errorHandler = errorHandler;
        this.taskNamePrefix = taskNamePrefix;
    }

    public void resetAndLoadFirstPage() {
        this.currentPage = 0;
        this.lastPageLoaded = false;
        this.loading = false;
        loadNextPage();
    }

    /**
     * Für ListView/Fx-Index-basierte Trigger (TransactionList, ChatPreview).
     */
    public void ensureLoadedForIndex(int visibleIndex, int totalSize, int prefetchThreshold) {
        if (loading || lastPageLoaded) {
            return;
        }
        if (visibleIndex >= totalSize - prefetchThreshold) {
            loadNextPage();
        }
    }

    /**
     * Für ScrollPane-basierte Trigger (Chat-Nachrichten).
     * Wird einfach aufgerufen, wenn dein Scroll-Schwellenwert erreicht ist.
     */
    public void ensureLoadedForScroll() {
        if (loading || lastPageLoaded) {
            return;
        }
        loadNextPage();
    }

    public boolean isLastPageLoaded() {
        return lastPageLoaded;
    }

    public boolean isLoading() {
        return loading;
    }

    private void loadNextPage() {
        if (loading || lastPageLoaded) {
            return;
        }
        loading = true;
        final int pageToLoad = currentPage;

        Task<Page<T>> task = new Task<>() {
            @Override
            protected Page<T> call() throws Exception {
                return pageFetcher.fetch(pageToLoad, pageSize);
            }
        };

        task.setOnSucceeded(ev -> {
            try {
                Page<T> page = task.getValue();
                pageConsumer.accept(page);
                lastPageLoaded = page.isLast();
                currentPage = pageToLoad + 1;
            } finally {
                loading = false;
            }
        });

        task.setOnFailed(ev -> {
            loading = false;
            Throwable ex = task.getException();
            if (errorHandler != null) {
                errorHandler.accept(ex);
            }
        });

        UiHelpers.startBackgroundTask(task, taskNamePrefix + pageToLoad);
    }
}
