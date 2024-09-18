package fr.usmb.process;

import com.google.common.eventbus.Subscribe;
import fr.usmb.EventBusService;
import fr.usmb.messages.BroadcastMessage;
import fr.usmb.messages.DedicatedMessage;
import fr.usmb.messages.Message;
import fr.usmb.messages.TokenMessage;
import fr.usmb.token.TokenState;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Getter
public class Communicator {

    @Getter
    private final int id = Communicator.nbProcess++;

    public static final int maxNbProcess = 3;
    private static int nbProcess = 0;

    @Getter(AccessLevel.PACKAGE)
    private final EventBusService bus;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private TokenState state;

    private final Process process;
    private final ProcessLogger logger;

    private final LamportClock clock;
    private final Semaphore semaphore;

    @Getter
    private final List<Message<?>> mailBox;

    public Communicator(Process process, ProcessLogger logger) {

        this.process = process;
        this.clock = new LamportClock();
        this.logger = logger;
        this.semaphore = new Semaphore(1);
        this.mailBox = new ArrayList<>();

        this.bus = EventBusService.getInstance();
        this.bus.registerSubscriber(this);

        this.state = TokenState.NULL;

    }

    /**
     * Increment the clock.
     */
    public void incClock() {
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
     *
     * @return {@link int} The clock.
     */
    public int getClock() {
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
     * @param data L'objet à diffuser
     */
    public <T> void broadcast(T data, boolean isSystemMessage) {
        try {

            BroadcastMessage<T> broadcastMessage = new BroadcastMessage<>(data);

            if (!isSystemMessage) {
                this.semaphore.acquire();
                this.clock.increment();
                broadcastMessage.setTimestamp(clock.get());
            }

            // Creating the broadcast message
            broadcastMessage.setSender(this.process.getName());

            this.logger.info("Broadcasting message: " + broadcastMessage.getMessage());
            this.bus.postEvent(broadcastMessage);

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
     *
     * @param data {@link Message} Le message à envoyer.
     */
    public <T> void broadcast(T data) {
        broadcast(data, false);
    }

    /**
     * Send a message to a specific process.
     *
     * @param to              {@link String} The name of the process to send the message to.
     * @param data            {@link Message} The message to send.
     * @param isSystemMessage {@link boolean} True if the message is a system message, false otherwise.
     * @param <T>             {@link T} The type of the message.
     */
    public <T> void sendTo(String to, T data, boolean isSystemMessage) {
        try {

            DedicatedMessage<T> dedicatedMessage = new DedicatedMessage<>(data);

            if (!isSystemMessage) {
                this.semaphore.acquire();
                this.clock.increment();
                dedicatedMessage.setTimestamp(clock.get());
            }
            // Creating the dedicated dedicatedMessage
            dedicatedMessage.setTimestamp(dedicatedMessage.getTimestamp());
            dedicatedMessage.setSender(this.process.getName());
            dedicatedMessage.setReceiver(to);

            this.logger.info("Sending dedicatedMessage: " + dedicatedMessage.getMessage() + " to " + to);
            this.bus.postEvent(dedicatedMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (!isSystemMessage) {
                this.semaphore.release();
            }
        }
    }

    /**
     * Send a message to a specific process.
     *
     * @param to   {@link String} The name of the process to send the message to.
     * @param data {@link Message} The message to send.
     * @param <T>  {@link T} The type of the message.
     */
    public <T> void sendTo(String to, T data) {
        sendTo(to, data, false);
    }

    /**
     * Send the token to the next process.
     *
     * @param tokenMessage {@link TokenMessage} The token message to send.
     */
    private void sendTokenToNextProcess(TokenMessage<?> tokenMessage) {
        String nextProcess = "P" + (this.getProcess().getId() + 1) % Communicator.maxNbProcess;
        tokenMessage.getToken().setHolder(nextProcess);
        sendTo(nextProcess, tokenMessage);
        this.bus.postEvent(tokenMessage);
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
        this.mailBox.add(message);
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
        this.mailBox.add(message);
        this.logger.info("Receiving message: " + message.getMessage() + " from " + message.getSender());
    }

    /**
     * This method is triggered when the tokenMessage is received.
     */
    @Subscribe
    private void onToken(TokenMessage<?> tokenMessage) throws InterruptedException {
        clock.update(tokenMessage.getTimestamp());
        if (!tokenMessage.getToken().getHolder().equalsIgnoreCase(this.process.getName())) return;

        if (this.state == TokenState.REQUEST) {
            this.state = TokenState.CRITICAL_SECTION;
            while (this.state != TokenState.RELEASE) {
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
        this.state = TokenState.REQUEST;
        while (this.state == TokenState.REQUEST) {
            Thread.sleep(500);
        }
    }

    /**
     * This method will release the token.
     */
    public void release() {
        this.state = TokenState.RELEASE;
    }

}
