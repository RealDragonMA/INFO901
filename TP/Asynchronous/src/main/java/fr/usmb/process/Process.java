package fr.usmb.process;

import fr.usmb.messages.Message;
import lombok.Getter;

/**
 * This class represents a process that runs in a separate thread and communicates with other
 * processes using an event bus. It implements the Runnable interface to allow multi-threading.
 */
public class Process implements Runnable {

    private final Thread thread;
    private boolean alive;
    private boolean dead;

    @Getter
    private final Communicator communicator;

    @Getter
    protected ProcessLogger logger;

    public Process(String name) {

        this.thread = new Thread(this);
        this.thread.setName(name);

        this.alive = true;
        this.dead = false;

        this.logger = new ProcessLogger(this);

        this.communicator = new Communicator(this.logger);

        this.thread.start();
    }


    /**
     * Run method for the process. This method is called when the thread is started.
     * It will run until the alive flag is set to false.
     */
    public void run() {
        int loop = 0;

        this.logger.info(Thread.currentThread().getName() + " id :" + this.getId());

        while (this.alive) {
            this.logger.info(Thread.currentThread().getName() + " Loop : " + loop);
            try {
                Thread.sleep(500);

                if (this.getName().equalsIgnoreCase("P0")) {
                    this.communicator.sendTo(1, "j'appelle 2 et je te recontacte après");

                    this.communicator.sendToSync(2, "J'ai laissé un message à 2, je le rappellerai après, on se sychronise tous et on attaque la partie ?");
                    Message<Object> msg = this.communicator.receiveFromSync(2);

                    this.communicator.sendToSync(1, "2 est OK pour jouer, on se synchronise et c'est parti!");

                    this.communicator.synchronize();

                    this.communicator.requestSC();
                    if (this.communicator.getMailBox().isEmpty()) {
                        this.logger.info("Catched !");
                        this.communicator.broadcast("J'ai gagné !!!");
                    } else {
                        Message<Object> message = this.communicator.getMailBox().getMessage();
                        this.logger.info(msg.getSender() + " à eu le jeton en premier");
                    }
                    this.communicator.releaseSC();

                }
                if (this.getName().equalsIgnoreCase("P1")) {
                    if (!this.communicator.getMailBox().isEmpty()) {
                        this.communicator.getMailBox().getMessage();
                        Message<Object> msg = this.communicator.receiveFromSync(0);

                        this.communicator.synchronize();

                        this.communicator.requestSC();
                        if (this.communicator.getMailBox().isEmpty()) {
                            this.logger.info("Catched !");
                            this.communicator.broadcast("J'ai gagné !!!");
                        } else {
                            msg = this.communicator.getMailBox().getMessage();
                            this.logger.info(msg.getSender() + " à eu le jeton en premier");
                        }
                        this.communicator.releaseSC();
                    }
                }
                if (this.getName().equalsIgnoreCase("P2")) {
                    Message<Object> msg = this.communicator.receiveFromSync(0);
                    this.communicator.sendToSync(0, "OK");

                    this.communicator.synchronize();

                    this.communicator.requestSC();
                    if (this.communicator.getMailBox().isEmpty()) {
                        this.logger.info("Catched !");
                        this.communicator.broadcast("J'ai gagné !!!");
                    } else {
                        msg = this.communicator.getMailBox().getMessage();
                        this.logger.info(msg.getSender() + " à eu le jeton en premier");
                    }
                    this.communicator.releaseSC();
                }


            } catch (Exception e) {
                this.logger.error("Error in process loop", e);
            }
        }

        this.logger.info(Thread.currentThread().getName() + " stopped");
        this.dead = true;
    }

    // =====================================
    //             Process Logic
    // =====================================

    /**
     * Wait until the process is fully stopped (has reached the dead state).
     */
    public void waitStopped() {
        while (!this.dead) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Stop the process by setting the alive flag to false.
     */
    public void stop() {
        this.alive = false;
    }

    // =====================================
    //                Utils
    // =====================================

    public String getName() {
        return this.thread.getName();
    }

    public MailBox getMailBox() {
        return this.communicator.getMailBox();
    }

    public int getId() {
        return this.communicator.getId();
    }
}
