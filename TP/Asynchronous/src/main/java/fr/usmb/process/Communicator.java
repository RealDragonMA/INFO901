package fr.usmb.process;

import com.google.common.eventbus.Subscribe;
import fr.usmb.messages.BroadcastMessage;
import fr.usmb.messages.DedicatedMessage;
import fr.usmb.messages.Message;
import fr.usmb.messages.TokenMessage;
import fr.usmb.token.TokenState;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.Semaphore;

@Getter
public class Communicator {

    private final Process process;
    private final ProcessLogger logger;

    private final LamportClock clock;
    private final Semaphore semaphore;

    private List<Message<?>> mailBox;

    public Communicator(Process process, LamportClock clock, ProcessLogger logger) {
        this.process = process;
        this.clock = clock;
        this.logger = logger;
        this.semaphore = new Semaphore(1);
    }

    /**
     * Increment the clock.
     */
    public void incClock(){
        try {
            this.semaphore.acquire();
            this.clock.increment();
        } catch (InterruptedException e) {
            this.logger.error("Error while incrementing the clock", e);
            Thread.currentThread().interrupt();
        } finally {
            this.semaphore.release();
        }
    }

    /**
     * Get the clock.
     * @return {@link int} The clock.
     */
    public int getClock(){
        try {
            this.semaphore.acquire();
            return this.clock.get();
        } catch (InterruptedException e) {
            this.logger.error("Error while getting the clock", e);
            Thread.currentThread().interrupt();
            return -1;
        } finally {
            this.semaphore.release();
        }
    }


    /**
     * Envoie un message à tous les autres processus (broadcast).
     * Seuls les messages non système modifient l'horloge de Lamport.
     *
     * @param message L'objet à diffuser
     */
    public void broadcast(Message<?> message, boolean isSystemMessage) {
        try {

            if (!isSystemMessage) {
                this.semaphore.acquire();
                this.clock.increment();
                message.setTimestamp(clock.get());
            }

            BroadcastMessage<?> broadcastMessage = new BroadcastMessage<>(message.getMessage());
            broadcastMessage.setTimestamp(message.getTimestamp());
            broadcastMessage.setSender(this.process.getThread().getName());

            this.logger.info("Broadcasting message: " + message.getMessage());
            this.process.getBus().postEvent(broadcastMessage);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (!isSystemMessage) {
                this.semaphore.release();
            }
        }
    }

    /**
     * Envoie un message à un processus spécifique.
     * @param message {@link Message} Le message à envoyer.
     */
    public void broadcast(Message<?> message){
        broadcast(message, false);
    }

    /**
     * Send a message to a specific process.
     *
     * @param to      {@link String} The name of the process to send the message to.
     * @param message {@link Message} The message to send.
     * @param <T>     {@link T} The type of the message.
     */
    public <T> void sendTo(String to, Message<T> message) {
        clock.increment();
        DedicatedMessage<T> dedicatedMessage = new DedicatedMessage<>(message.getMessage());
        // Init the timestamp of the message
        dedicatedMessage.setTimestamp(clock.get());
        // Init the sender of the message
        dedicatedMessage.setSender(this.process.getThread().getName());
        // Init the receiver of the message
        dedicatedMessage.setReceiver(to);
        this.logger.info("Sending message: " + message.getMessage() + " to " + to);
        this.process.getBus().postEvent(dedicatedMessage);
    }

    /**
     * Send the token to the next process.
     *
     * @param tokenMessage {@link TokenMessage} The token message to send.
     */
    private void sendTokenToNextProcess(TokenMessage<?> tokenMessage) {
        String nextProcess = "P" + (this.getProcess().getId() + 1) % Process.maxNbProcess;
        tokenMessage.getToken().setHolder(nextProcess);
        sendTo(nextProcess, tokenMessage);
        this.getProcess().getBus().postEvent(tokenMessage);
    }

    /**
     * Method to handle broadcast messages.
     *
     * @param message {@link BroadcastMessage} The message to handle.
     */
    @Subscribe
    private void onBroadcast(BroadcastMessage<?> message) {
        clock.update(message.getTimestamp());
        if (message.getSender().equalsIgnoreCase(this.process.getName())) return;
        this.logger.info("Receiving broadcast message: " + message.getMessage() + " from " + message.getSender());
    }

    /**
     * Method to handle dedicated messages.
     *
     * @param message {@link DedicatedMessage} The message to handle.
     */
    @Subscribe
    private void onReceive(DedicatedMessage<?> message) {
        clock.update(message.getTimestamp());
        if (!message.getReceiver().equalsIgnoreCase(this.process.getName())) return;
        this.logger.info("Receiving message: " + message.getMessage() + " from " + message.getSender());
    }

    /**
     * This method is triggered when the tokenMessage is received.
     */
    @Subscribe
    private void onToken(TokenMessage<?> tokenMessage) throws InterruptedException {
        clock.update(tokenMessage.getTimestamp());
        if (!tokenMessage.getToken().getHolder().equalsIgnoreCase(this.process.getName())) return;

        if (this.getProcess().getState() == TokenState.REQUEST) {
            this.process.setState(TokenState.CRITICAL_SECTION);
            while (this.getProcess().getState() != TokenState.RELEASE) {
                Thread.sleep(500);
            }
            this.logger.info("Releasing the token");
        }

        sendTokenToNextProcess(tokenMessage);
    }

    /**
     * This method will request the token. And stop the process until the token is received.
     */
    public void requestSC() throws InterruptedException {
        this.getProcess().setState(TokenState.REQUEST);
        while (this.getProcess().getState() == TokenState.REQUEST) {
            Thread.sleep(500);
        }
    }

    /**
     * This method will release the token.
     */
    public void release() {
        this.getProcess().setState(TokenState.RELEASE);
    }

}
