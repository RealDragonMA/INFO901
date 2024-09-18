package fr.usmb.process;

import fr.usmb.EventBusService;
import fr.usmb.token.TokenState;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a process that runs in a separate thread and communicates with other
 * processes using an event bus. It implements the Runnable interface to allow multi-threading.
 */
public class Process implements Runnable {

    public static final int maxNbProcess = 3;
    private static int nbProcess = 0;

    @Getter(AccessLevel.PACKAGE)
    private final Thread thread;

    @Getter(AccessLevel.PACKAGE)
    private EventBusService bus;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private TokenState state;

    @Getter
    private final Communicator communicator;

    @Getter
    private final int id = Process.nbProcess++;

    @Getter
    protected ProcessLogger logger;

    private boolean alive;
    private boolean dead;

    public Process(String name) {
        this.bus = EventBusService.getInstance();
        this.bus.registerSubscriber(this);

        this.thread = new Thread(this);
        this.thread.setName(name);

        this.alive = true;
        this.dead = false;

        this.state = TokenState.NULL;

        this.logger = new ProcessLogger(this);

        this.communicator = new Communicator(this, new LamportClock(), this.logger);

        this.thread.start();
    }


    /**
     * Run method for the process. This method is called when the thread is started.
     * It will run until the alive flag is set to false.
     */
    public void run() {
        this.logger.info(Thread.currentThread().getName() + " id: " + this.id + " started !");
        while (this.alive) {
            try {
                Thread.sleep(500);
                if (Thread.currentThread().getName().equals("P1")) {
                    System.out.println();
                }
            } catch (Exception e) {
                this.logger.error("Error while sleeping", e);
                Thread.currentThread().interrupt();
            }
        }
        cleanup();
        this.logger.info(Thread.currentThread().getName() + " stopped");
    }

    // =====================================
    //             Process Logic
    // =====================================

    /**
     * Clean up resources and unregister the process from the EventBus.
     */
    private void cleanup() {
        this.bus.unRegisterSubscriber(this);
        this.bus = null;
        this.dead = true;
    }

    /**
     * Wait until the process is fully stopped (has reached the dead state).
     */
    public void waitStopped() {
        while (!this.dead) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                this.logger.error("Error while sleeping", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Stop the process by setting the alive flag to false.
     */
    public void stop() {
        this.alive = false;
        this.logger.info("Process stopped");
    }

    public String getName() {
        return this.thread.getName();
    }
}
