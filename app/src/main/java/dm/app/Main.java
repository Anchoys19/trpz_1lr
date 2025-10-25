package dm.app;

import dm.commands.*;
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
        CommandManager cmdManager = new CommandManager(); // Command Pattern

        System.out.println("Download Manager with Command Pattern. Commands:");
        System.out.println(" add <url> <file>      - додати завантаження");
        System.out.println(" pause <id>            - призупинити (через Command)");
        System.out.println(" resume <id>           - відновити (через Command)");
        System.out.println(" list [filter]         - показати список");
        System.out.println(" limit <bytes_per_sec> - встановити ліміт");
        System.out.println(" undo                  - скасувати останню команду");
        System.out.println(" redo                  - повторити команду");
        System.out.println(" history               - показати історію команд");
        System.out.println(" exit                  - вихід");

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
                            if (parts.length < 3) {
                                System.out.println("Usage: add <url> <file>");
                                break;
                            }
                            int id = svc.add(parts[1], Path.of(parts[2]));
                            System.out.println("Task created: #" + id);
                        }

                        case "pause" -> {
                            if (parts.length < 2) {
                                System.out.println("Usage: pause <id>");
                                break;
                            }
                            int id = Integer.parseInt(parts[1]);

                            // Використовуємо Command Pattern
                            Command pauseCmd = new DownloadCommand(
                                    svc, id, DownloadCommand.Action.PAUSE
                            );
                            cmdManager.execute(pauseCmd);
                        }

                        case "resume" -> {
                            if (parts.length < 2) {
                                System.out.println("Usage: resume <id>");
                                break;
                            }
                            int id = Integer.parseInt(parts[1]);

                            // Використовуємо Command Pattern
                            Command resumeCmd = new DownloadCommand(
                                    svc, id, DownloadCommand.Action.RESUME
                            );
                            cmdManager.execute(resumeCmd);
                        }

                        case "undo" -> {
                            cmdManager.undo();
                        }

                        case "redo" -> {
                            cmdManager.redo();
                        }

                        case "history" -> {
                            cmdManager.showHistory();
                        }

                        case "list" -> {
                            String mode = (parts.length >= 2) ? parts[1].toLowerCase(Locale.ROOT) : "all";
                            int batch = (parts.length >= 3) ? Math.max(1, Integer.parseInt(parts[2])) : 50;

                            var base = new TaskIterable(svc.getRepository(), batch);
                            Iterable<DownloadTask> iterable;

                            switch (mode) {
                                case "completed" -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.COMPLETED);
                                case "paused"    -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.PAUSED);
                                case "running"   -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.RUNNING);
                                case "error"     -> iterable = new FilteredTaskIterable(base, t -> t.status == DownloadTask.Status.ERROR);
                                default          -> iterable = base;
                            }

                            for (DownloadTask t : iterable) {
                                String size = (t.totalBytes >= 0) ? (t.lastByte + "/" + t.totalBytes) : (t.lastByte + "/-1");
                                System.out.printf("#%d [%s] %s (%s) -> %s%n",
                                        t.id, t.status, t.url, size, t.target);
                            }
                        }

                        case "limit" -> {
                            if (parts.length < 2) {
                                System.out.println("Usage: limit <bytes_per_sec|0>");
                                break;
                            }
                            long lim = Long.parseLong(parts[1]);
                            svc.setLimit(lim);
                            System.out.println("Limit set to " + lim + " B/s");
                        }

                        case "exit" -> {
                            svc.close();
                            return;
                        }

                        default -> System.out.println("Unknown command");
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}