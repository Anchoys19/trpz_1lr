package dm.core;

public class TaskIterable implements Iterable<DownloadTask> {
    private final TaskRepository repo;
    private final int batchSize;

    public TaskIterable(TaskRepository repo, int batchSize) {
        this.repo = repo;
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public java.util.Iterator<DownloadTask> iterator() {
        return new TaskIterator(repo, batchSize);
    }
}
