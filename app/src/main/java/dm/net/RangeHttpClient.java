package dm.net;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.function.LongSupplier;

public class RangeHttpClient {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(ProxySelector.getDefault())
            .build();

    public static final class Result {
        public final long contentLength;
        public final boolean supportsRange;
        public Result(long contentLength, boolean supportsRange) {
            this.contentLength = contentLength; this.supportsRange = supportsRange;
        }
    }

    /** Завантаження у файл з можливістю відновлення (Range) і простим тротлінгом. */
    public Result download(String url, Path target, long startAt,
                           ProgressListener progress, InterruptFlag stopFlag,
                           LongSupplier limitBps) throws Exception {

        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "DownloadManager/1.0 (+java)")
                .GET();
        if (startAt > 0) rb.header("Range", "bytes=" + startAt + "-");

        HttpRequest req = rb.build();
        HttpResponse<java.io.InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        int code = resp.statusCode();
        if (code != 200 && code != 206) {
            throw new IOException("HTTP " + code + " while downloading: " + url);
        }

        long clenHeader = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
        boolean supportsRange = code == 206
                || resp.headers().firstValue("Accept-Ranges").map("bytes"::equalsIgnoreCase).orElse(false);

        Files.createDirectories(target.toAbsolutePath().getParent());

        OpenOption[] opts = (startAt > 0)
                ? new OpenOption[]{ StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND }
                : new OpenOption[]{ StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };

        try (FileChannel fc = FileChannel.open(target, opts);
             var in = resp.body();
             ReadableByteChannel ch = Channels.newChannel(in)) {

            long knownTotal = (clenHeader > 0 && startAt > 0) ? (clenHeader + startAt) : clenHeader;
            ByteBuffer buf = ByteBuffer.allocate(64 * 1024);

            long windowStart = System.nanoTime();
            long bytesInWindow = 0;

            while (ch.read(buf) != -1) {
                if (stopFlag.isSet()) break;
                buf.flip();
                while (buf.hasRemaining()) {
                    int w = fc.write(buf);
                    bytesInWindow += w;
                }
                buf.clear();

                long written = fc.size();
                progress.onProgress(written, knownTotal);

                long limit = (limitBps != null) ? limitBps.getAsLong() : 0L;
                if (limit > 0) {
                    long elapsedNs = System.nanoTime() - windowStart;
                    double seconds = elapsedNs / 1_000_000_000.0;
                    if (seconds > 0 && (bytesInWindow / seconds) > limit) {
                        long expectedNs = (long)((bytesInWindow * 1_000_000_000.0) / limit);
                        long sleepNs = expectedNs - elapsedNs;
                        if (sleepNs > 0) {
                            Thread.sleep(Math.min(250, sleepNs / 1_000_000));
                        }
                    }
                    if (seconds >= 1.0) {
                        windowStart = System.nanoTime();
                        bytesInWindow = 0;
                    }
                }
            }
        }

        long totalLen = (clenHeader > 0 && startAt > 0) ? (clenHeader + startAt) : clenHeader;
        return new Result(totalLen, supportsRange);
    }

    public interface ProgressListener { void onProgress(long bytesTotal, long contentLength); }

    public static final class InterruptFlag {
        private volatile boolean set=false;
        public void stop(){ set=true; }
        public boolean isSet(){ return set; }
        public void reset(){ set=false; }
    }
}
