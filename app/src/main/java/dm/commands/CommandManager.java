package dm.commands;

import java.util.Stack;

public class CommandManager {
    private final Stack<Command> history = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    public void execute(Command command) throws Exception {
        command.execute();
        history.push(command);
        redoStack.clear();
    }

    public void undo() throws Exception {
        if (history.isEmpty()) {
            System.out.println("Nothing to undo");
            return;
        }

        Command cmd = history.pop();
        cmd.undo();
        redoStack.push(cmd);
        System.out.println("Undone: " + cmd.getDescription());
    }

    public void redo() throws Exception {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo");
            return;
        }

        Command cmd = redoStack.pop();
        cmd.execute();
        history.push(cmd);
        System.out.println("Redone: " + cmd.getDescription());
    }

    public void showHistory() {
        if (history.isEmpty()) {
            System.out.println("History is empty");
            return;
        }

        System.out.println("Command history:");
        int i = 1;
        for (Command cmd : history) {
            System.out.println(i++ + ". " + cmd.getDescription());
        }
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}