package dm.core;

import dm.net.RangeHttpClient;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

public class DownloadService implements AutoCloseable {
    private final TaskRepository repo;
    private final RangeHttpClient http = new RangeHttpClient();
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Map<Integer, RangeHttpClient.InterruptFlag> flags = new ConcurrentHashMap<>();
    private final BandwidthPolicy policy = new BandwidthPolicy();

    public DownloadService(Path sqliteDb) throws Exception {
        this.repo = new TaskRepository(sqliteDb);
    }


    public TaskRepository getRepository() {
        return repo;
    }


    public int add(String url, Path target) throws Exception {
        int id = repo.create(url, target.toString());
        resume(id);
        return id;
    }

    public void resume(int id) throws Exception {
        DownloadTask t = repo.findById(id);
        if (t == null) throw new IllegalArgumentException("No such task: " + id);

        RangeHttpClient.InterruptFlag flag = new RangeHttpClient.InterruptFlag();
        flags.put(id, flag);

        repo.updateStatus(id, DownloadTask.Status.RUNNING, t.lastByte);

        pool.submit(() -> {
            try {
                LongSupplier lim = policy::getLimit;
                RangeHttpClient.Result r = http.download(
                        t.url, t.target, t.lastByte,
                        (bytes, total) -> {
                            try { repo.updateProgress(id, bytes, total); }
                            catch (SQLException e) { /* лог за потреби */ }
                        },
                        flag, lim);

                repo.updateStatus(id, DownloadTask.Status.COMPLETED, r.contentLength > 0 ? r.contentLength : t.lastByte);
            } catch (Exception e) {
                try { repo.updateStatus(id, DownloadTask.Status.ERROR, t.lastByte); }
                catch (SQLException ignored) {}
                System.out.printf("java error: %s%n", e.toString());
            }
        });
    }

    public void pause(int id) throws Exception {
        RangeHttpClient.InterruptFlag f = flags.get(id);
        if (f != null) f.stop();
        DownloadTask t = repo.findById(id);
        if (t != null) {
            // lastByte зафіксується в onProgress при наступному циклі або як є
            repo.updateStatus(id, DownloadTask.Status.PAUSED, t.lastByte);
        }
    }

    public void printList() throws Exception {
        List<DownloadTask> all = repo.listAll();
        for (DownloadTask t : all) {
            String size = (t.totalBytes >= 0) ? (t.totalBytes + "/" + t.totalBytes) : "0/-1";
            System.out.printf("#%d [%s] %s (%s) -> %s%n",
                    t.id, t.status, t.url, size, t.target);
        }
    }

    public void setLimit(long bytesPerSec) { policy.setLimit(bytesPerSec); }

    @Override public void close() throws Exception {
        pool.shutdownNow();
        repo.close();
    }
}
