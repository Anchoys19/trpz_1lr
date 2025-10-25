package dm.commands;

import dm.core.DownloadService;
import dm.core.DownloadTask;

public class DownloadCommand implements Command {
    private final DownloadService service;
    private final int taskId;
    private final Action action;
    private DownloadTask.Status previousStatus;

    public enum Action { PAUSE, RESUME }

    public DownloadCommand(DownloadService service, int taskId, Action action) {
        this.service = service;
        this.taskId = taskId;
        this.action = action;
    }

    @Override
    public void execute() throws Exception {
        DownloadTask task = service.getRepository().findById(taskId);
        if (task != null) {
            previousStatus = task.status;
        }

        if (action == Action.PAUSE) {
            service.pause(taskId);
            System.out.println("Paused task #" + taskId);
        } else {
            service.resume(taskId);
            System.out.println("Resumed task #" + taskId);
        }
    }

    @Override
    public void undo() throws Exception {
        if (action == Action.PAUSE && previousStatus == DownloadTask.Status.RUNNING) {
            service.resume(taskId);
            System.out.println("Undo: Resumed task #" + taskId);
        } else if (action == Action.RESUME && previousStatus == DownloadTask.Status.PAUSED) {
            service.pause(taskId);
            System.out.println("Undo: Paused task #" + taskId);
        }
    }

    @Override
    public String getDescription() {
        return action + " task #" + taskId;
    }
}