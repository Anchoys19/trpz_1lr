package dm.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaskIterator implements Iterator<DownloadTask> {
    private final TaskRepository repo;
    private final int batchSize;
    private int cursor = 0;
    private final List<DownloadTask> buffer = new ArrayList<>();
    private boolean noMoreData = false;

    public TaskIterator(TaskRepository repo, int batchSize) {
        this.repo = repo;
        this.batchSize = batchSize;
    }

    @Override
    public boolean hasNext() {
        if (!buffer.isEmpty()) return true;
        if (noMoreData) return false;
        loadBatch();
        return !buffer.isEmpty();
    }

    @Override
    public DownloadTask next() {
        if (!hasNext()) throw new java.util.NoSuchElementException();
        return buffer.remove(0);
    }

    private void loadBatch() {
        try {
            List<DownloadTask> chunk = repo.listRange(cursor, batchSize);
            if (chunk.isEmpty()) {
                noMoreData = true;
                return;
            }
            buffer.addAll(chunk);
            cursor += chunk.size();
        } catch (SQLException e) {
            noMoreData = true;
            throw new RuntimeException("DB error while iterating tasks", e);
        }
    }
}
