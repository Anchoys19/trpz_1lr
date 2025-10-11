package dm.core;

import java.nio.file.Path;

public class DownloadTask {
    public enum Status { NEW, RUNNING, PAUSED, COMPLETED, ERROR }

    public int id;
    public String url;
    public Path target;
    public Status status;
    public long lastByte;
    public long totalBytes;

    public DownloadTask(int id, String url, Path target, Status status, long lastByte, long totalBytes) {
        this.id = id;
        this.url = url;
        this.target = target;
        this.status = status;
        this.lastByte = lastByte;
        this.totalBytes = totalBytes;
    }
}
