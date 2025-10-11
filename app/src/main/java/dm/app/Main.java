package dm.app;

import dm.core.DownloadService;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        DownloadService svc = new DownloadService(Path.of("app/download.db"));

        System.out.println("Download Manager (console demo). Commands:");
        System.out.println(" add <url> <file>");
        System.out.println(" pause <id>");
        System.out.println(" resume <id>");
        System.out.println(" list");
        System.out.println(" limit <bytes_per_sec|0>");
        System.out.println(" exit");

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();

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
                        case "list" -> svc.printList();
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
