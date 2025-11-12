package org.fpj.Data;

public final class FxSlicePager<D, VM> {
    public interface Loader<D> { org.springframework.data.domain.Slice<D> load(org.springframework.data.domain.PageRequest pr); }
    public interface Mapper<D, VM> { VM map(D domain); }

    private final Loader<D> loader;
    private final Mapper<D, VM> mapper;
    private final int pageSize;
    private final java.util.concurrent.Executor io;
    private final javafx.collections.ObservableList<VM> items = javafx.collections.FXCollections.observableArrayList();

    private int pageIndex = 0;
    private boolean hasNext = true;
    private boolean loading = false;

    public FxSlicePager(Loader<D> loader, Mapper<D, VM> mapper, int pageSize, java.util.concurrent.Executor io) {
        this.loader = loader; this.mapper = mapper; this.pageSize = pageSize; this.io = io;
    }

    public javafx.collections.ObservableList<VM> items() { return items; }
    public boolean isLoading() { return loading; }
    public boolean hasNext() { return hasNext; }

    public void loadFirst() {
        pageIndex = 0; hasNext = true; items.clear();
        loadNext();
    }

    public void loadNext() {
        if (loading || !hasNext) return;
        loading = true;
        int idx = pageIndex;
        io.execute(() -> {
            var slice = loader.load(org.springframework.data.domain.PageRequest.of(idx, pageSize));
            var mapped = slice.getContent().stream().map(mapper::map).toList();
            boolean next = slice.hasNext();
            javafx.application.Platform.runLater(() -> {
                items.addAll(mapped);
                hasNext = next;
                if (next) pageIndex = idx + 1;
                loading = false;
            });
        });
    }
}
