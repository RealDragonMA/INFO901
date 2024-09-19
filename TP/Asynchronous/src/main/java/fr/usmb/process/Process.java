package fr.usmb.process;

import fr.usmb.messages.Message;
import lombok.Getter;

import java.util.List;

/**
 * This class represents a process that runs in a separate thread and communicates with other
 * processes using an event bus. It implements the Runnable interface to allow multi-threading.
 */
public class Process implements Runnable {

    private Thread thread;
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

        this.communicator = new Communicator(this, this.logger);

        this.thread.start();
    }


    /**
     * Run method for the process. This method is called when the thread is started.
     * It will run until the alive flag is set to false.
     */
    public void run() {
        int loop = 0;

        System.out.println(Thread.currentThread().getName() + " id :" + this.getId());

        while (this.alive) {
            try {
                Thread.sleep(500);

                if (this.getName().equals("P0")) {
//                    this.communicator.sendTo("j'appelle 2 et je te recontacte après", 1);
//
//                    this.communicator.sendToSync("J'ai laissé un message à 2, je le rappellerai après, on se sychronise tous et on attaque la partie ?", 2);
//                    this.communicator.recevFromSync(msg, 2);
//
//                    this.communicator.sendToSync("2 est OK pour jouer, on se synchronise et c'est parti!", 1);
//
//                    this.communicator.synchronize();
//
//                    this.communicator.requestSC();
//                    if (this.getMailBox().isEmpty()) {
//                        System.out.println("Catched !");
//                        this.communicator.broadcast("J'ai gagné !!!");
//                    } else {
//                        msg = this.communicator.mailbox.getMsg();
//                        System.out.println(str(msg.getSender()) + " à eu le jeton en premier");
//                    }
//                    this.communicator.releaseSC();

                }
                if (this.getName() == "P1") {
//                    if (!this.getMailBox().isEmpty()) {
//                        this.communicator.mailbox.getMessage();
//                        this.communicator.recevFromSync(msg, 0);
//
//                        this.communicator.synchronize();
//
//                        this.communicator.requestSC();
//                        if (this.communicator.mailbox.isEmpty()) {
//                            print("Catched !");
//                            this.communicator.broadcast("J'ai gagné !!!");
//                        } else {
//                            msg = this.communicator.mailbox.getMsg();
//                            print(str(msg.getSender()) + " à eu le jeton en premier");
//                        }
//                        this.communicator.releaseSC();
//                    }
                }
                if (this.getName() == "P2") {
//                    this.communicator.recevFromSync(msg, 0);
//                    this.communicator.sendToSync("OK", 0);
//
//                    this.communicator.synchronize();
//
//                    this.communicator.requestSC();
//                    if (this.communicator.mailbox.isEmpty()) {
//                        print("Catched !");
//                        this.communicator.broadcast("J'ai gagné !!!");
//                    } else {
//                        msg = this.communicator.mailbox.getMsg();
//                        print(str(msg.getSender()) + " à eu le jeton en premier");
//                    }
//                    this.communicator.releaseSC();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            loop++;
        }

        System.out.println(Thread.currentThread().getName() + " stopped");
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

    public List<Message<?>> getMailBox() {
        return this.communicator.getMailBox();
    }

    public int getId() {
        return this.communicator.getId();
    }
}
