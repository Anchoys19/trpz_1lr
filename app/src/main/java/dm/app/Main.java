package dm.app;

import dm.core.DownloadService;
import dm.core.DownloadTask;
import dm.core.TaskIterable;
import dm.core.FilteredTaskIterable;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        DownloadService svc = new DownloadService(Path.of("download.db"));

        System.out.println("Download Manager (console demo). Commands:");
        System.out.println(" add <url> <file>");
        System.out.println(" pause <id>");
        System.out.println(" resume <id>");
        System.out.println(" list [all|completed|paused|running|error] [batchSize]");
        System.out.println(" limit <bytes_per_sec|0>");
        System.out.println(" exit");

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase(Locale.ROOT);

                try {
                    switch (cmd) {
                        case "add" -> {
                            if (parts.length < 3) { System.out.println("Usage: add <url> <file>"); break; }
                            int id = svc.add(parts[1], Path.of(parts[2]));
                            System.out.println("Task created: #" + id);
                        }
                        case "pause" -> {
                            if (parts.length < 2) { System.out.println("Usage: pause <id>"); break; }
                            int id = Integer.parseInt(parts[1]);
                            svc.pause(id);
                            System.out.println("Paused #" + id);
                        }
                        case "resume" -> {
                            if (parts.length < 2) { System.out.println("Usage: resume <id>"); break; }
                            int id = Integer.parseInt(parts[1]);
                            svc.resume(id);
                            System.out.println("Resumed #" + id);
                        }
                        case "list" -> {
                            // list [all|completed|paused|running|error] [batchSize]
                            String mode = (parts.length >= 2) ? parts[1].toLowerCase(Locale.ROOT) : "all";
                            int batch = (parts.length >= 3) ? Math.max(1, Integer.parseInt(parts[2])) : 50;

                            var base = new TaskIterable(svc.getRepository(), batch);

                            Iterable<DownloadTask> iterable;
                            switch (mode) {
                                case "completed" -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.COMPLETED);
                                case "paused"    -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.PAUSED);
                                case "running"   -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.RUNNING);
                                case "error"     -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.ERROR);
                                case "all"       -> iterable = base;
                                default -> {
                                    System.out.println("Usage: list [all|completed|paused|running|error] [batchSize]");
                                    iterable = base;
                                }
                            }

                            for (DownloadTask t : iterable) {
                                String size = (t.totalBytes >= 0) ? (t.totalBytes + "/" + t.totalBytes) : "0/-1";
                                System.out.printf("#%d [%s] %s (%s) -> %s%n",
                                        t.id, t.status, t.url, size, t.target);
                            }
                        }
                        case "limit" -> {
                            if (parts.length < 2) { System.out.println("Usage: limit <bytes_per_sec|0>"); break; }
                            long lim = Long.parseLong(parts[1]);
                            svc.setLimit(lim);
                            System.out.println("Limit set to " + lim + " B/s (0 = unlimited)");
                        }
                        case "exit" -> { svc.close(); return; }
                        default -> System.out.println("Unknown command");
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}
