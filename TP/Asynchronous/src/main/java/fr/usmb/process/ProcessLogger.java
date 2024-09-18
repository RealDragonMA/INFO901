package fr.usmb.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProcessLogger {

    private Process process;

    /**
     * Logs a message with the thread name prepended for context.
     * @param message The message to log.
     */
    public void info(String message) {
        System.out.println("[Process " + process.getThread().getName() + "] " + message);
    }

    /**
     * Logs an error message with an exception.
     * @param message The error message.
     * @param e The exception that was thrown.
     */
    public void error(String message, Exception e) {
        System.err.println("[Process " + process.getThread().getName() + "] ERROR: " + message);
        e.printStackTrace();
    }

}
