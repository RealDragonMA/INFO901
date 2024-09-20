package fr.usmb.process;

import com.google.common.eventbus.Subscribe;
import fr.usmb.EventBusService;
import fr.usmb.messages.*;
import fr.usmb.token.Token;
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

    private final String name;
    private final ProcessLogger logger;

    private final LamportClock clock;
    private final Semaphore semaphore;

    @Getter
    private final MailBox mailBox;
    private final List<String> syncReceived;

    public Communicator(ProcessLogger logger) {

        this.clock = new LamportClock();
        this.logger = logger;
        this.semaphore = new Semaphore(1);
        this.mailBox = new MailBox();
        
        this.name = "P" + this.id;

        this.bus = EventBusService.getInstance();
        this.bus.registerSubscriber(this);

        this.state = TokenState.NULL;
        this.syncReceived = new ArrayList<>();

    }

    /**
     * Increments the Lamport clock. This method ensures mutual exclusion by acquiring a semaphore
     * before incrementing the clock. It is used to ensure that the clock is updated consistently
     * across multiple threads.
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
     * Retrieves the current value of the Lamport clock. This method is protected by a semaphore to
     * ensure that the clock is accessed safely in a concurrent environment.
     *
     * @return The current value of the Lamport clock.
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
     * Sends a broadcast message to all processes. The Lamport clock is incremented unless the message
     * is a system message, in which case the clock remains unaffected. This method posts the message
     * to the event bus for delivery.
     *
     * @param data The data to broadcast.
     * @param isSystemMessage True if the message is a system message, false otherwise.
     * @param <T> The type of the message payload.
     */
    public <T> void broadcast(T data, boolean isSystemMessage) {
        try {

            BroadcastMessage<T> broadcastMessage = new BroadcastMessage<>(data, this.name);

            if (!isSystemMessage) {
                this.semaphore.acquire();
                this.clock.increment();
                broadcastMessage.setTimestamp(clock.get());
            }

            // Creating the broadcast message
            broadcastMessage.setSender(this.name);

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
     * Sends a broadcast message to all processes, treating the message as a user message by default.
     * This will increment the Lamport clock and post the message to the event bus for delivery.
     *
     * @param data The data to broadcast.
     * @param <T> The type of the message payload.
     */
    public <T> void broadcast(T data) {
        broadcast(data, false);
    }

    /**
     * Sends a message to a specific process. The Lamport clock is incremented unless the message
     * is a system message. This method acquires a semaphore to ensure safe access to the clock
     * and posts the message to the event bus for delivery.
     *
     * @param to The ID of the destination process.
     * @param data The message to send.
     * @param isSystemMessage True if the message is a system message, false otherwise.
     * @param <T> The type of the message payload.
     */
    public <T> void sendTo(int to, T data, boolean isSystemMessage) {
        try {

            DedicatedMessage<T> dedicatedMessage = new DedicatedMessage<>(data, this.name, String.valueOf(to));

            if (!isSystemMessage) {
                this.semaphore.acquire();
                this.clock.increment();
                dedicatedMessage.setTimestamp(clock.get());
            }
            // Creating the dedicated dedicatedMessage
            dedicatedMessage.setTimestamp(dedicatedMessage.getTimestamp());
            dedicatedMessage.setSender(this.name);
            dedicatedMessage.setReceiver(String.valueOf(to));

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
     * Sends a message to a specific process, treating the message as a user message by default.
     * This will increment the Lamport clock and post the message to the event bus for delivery.
     *
     * @param to The ID of the destination process.
     * @param data The message to send.
     * @param <T> The type of the message payload.
     */
    public <T> void sendTo(int to, T data) {
        sendTo(to, data, false);
    }


    /**
     * Sends a synchronous broadcast message to all processes. If the current process is the sender,
     * it waits for acknowledgment from all other processes. Otherwise, it waits for the broadcast
     * message to arrive. This method blocks the calling thread until synchronization is complete.
     *
     * @param data The data to broadcast synchronously.
     * @param from The ID of the sending process.
     * @param <T> The type of the message payload.
     */
    public <T> void broadcastSync(T data, int from) {
        if (this.id == from) {
            try {
                BroadcastMessage<T> broadcastMessage = new BroadcastMessage<>(data, this.name);

                // Envoyer le message et incrémenter l'horloge
                synchronized (this) {
                    this.clock.increment();
                    broadcastMessage.setTimestamp(clock.get());
                }

                broadcastMessage.setSender(this.name);
                this.logger.info("Broadcasting synchronous message: " + broadcastMessage.getMessage());
                this.bus.postEvent(broadcastMessage);

                // Attendre que tous les processus confirment la réception
                synchronized (syncReceived) {
                    while (syncReceived.size() < Communicator.maxNbProcess - 1) {
                        syncReceived.wait();
                    }
                }

                syncReceived.clear(); // Réinitialiser pour la prochaine synchronisation

                this.logger.info("Synchronous broadcast completed. All processes acknowledged receipt.");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            // Si ce n'est pas le processus 'from', il doit attendre de recevoir le message
            synchronized (syncReceived) {
                while (!syncReceived.contains(this.name)) {
                    try {
                        syncReceived.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        this.logger.error("Error while waiting for synchronous broadcast", e);
                    }
                }
            }
        }
    }


    /**
     * Sends a message synchronously to a specific process. The sender will wait until the destination
     * process acknowledges receipt of the message. This method blocks the calling thread until
     * the acknowledgment is received.
     *
     * @param dest The ID of the destination process.
     * @param data The message to send.
     * @param <T> The type of the message payload.
     */
    public <T> void sendToSync(int dest, T data) {
        String destProcessName = "P" + dest;

        try {
            DedicatedMessage<T> dedicatedMessage = new DedicatedMessage<>(data, this.name, destProcessName);

            // Envoyer le message et incrémenter l'horloge
            synchronized (this) {
                this.clock.increment();
                dedicatedMessage.setTimestamp(clock.get());
            }

            dedicatedMessage.setSender(this.name);
            dedicatedMessage.setReceiver(destProcessName);

            this.logger.info("Sending synchronous message: " + dedicatedMessage.getMessage() + " to " + destProcessName);
            this.bus.postEvent(dedicatedMessage);

            // Attendre que le processus destinataire accuse réception
            synchronized (syncReceived) {
                while (!syncReceived.contains(destProcessName)) {
                    syncReceived.wait();
                }
            }

            syncReceived.clear(); // Réinitialiser après la réception

            this.logger.info("Synchronous send completed. Process " + destProcessName + " acknowledged receipt.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }




    /**
     * Sends the token to the next process in the logical ring. This method posts a system message
     * (token) to the event bus without incrementing the Lamport clock, as token messages are system-related.
     *
     * @param tokenMessage The token message to send to the next process.
     */
    private void sendTokenToNextProcess(TokenMessage<?> tokenMessage) {
        int nextProcess = (this.id + 1) % Communicator.maxNbProcess;
        tokenMessage.getToken().setHolder(String.valueOf(nextProcess));
        sendTo(nextProcess, tokenMessage, true);
        this.logger.info("Sending the token to " + nextProcess);
        this.bus.postEvent(tokenMessage);
    }

    /**
     * Synchronizes the current process with all other processes. This method sends a synchronization
     * message to all processes and blocks until all processes have reached the synchronization point.
     * It waits for acknowledgment from all other processes before proceeding.
     */
    public void synchronize(){
        SynchronizedMessage syncMessage = new SynchronizedMessage(this.name);
        this.broadcast(syncMessage, true);

        synchronized (syncReceived){
            while (syncReceived.size() < Communicator.maxNbProcess - 1){
                try {
                    syncReceived.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    this.logger.error("Error while waiting for synchronization", e);
                }
            }
        }

        syncReceived.clear();
        this.logger.info("Process " + this.name + " is synchronized with all other processes");
    }

    /**
     * Receives a synchronous message from a specific process. This method blocks until a message
     * from the specified process arrives in the mailbox. Once the message is received, it is removed
     * from the mailbox and returned to the caller.
     *
     * @param from The ID of the process from which the message is expected.
     * @return The message received from the specified process.
     * @param <T> The type of the message payload.
     */
    public <T> Message<T> receiveFromSync(int from) {
        String fromProcessName = "P" + from;
        Message<T> receivedMessage = null;

        synchronized (mailBox) {
            // Attendre jusqu'à recevoir un message provenant du processus "from"
            while (receivedMessage == null) {
                try {
                    // Parcourir la boîte aux lettres pour trouver un message venant de "fromProcessName"
                    for (Message<?> msg : mailBox.getMessages()) {
                        if (msg.getSender().equalsIgnoreCase(fromProcessName)) {
                            receivedMessage = (Message<T>) msg;
                            break; // Quitter la boucle une fois le message trouvé
                        }
                    }

                    if (receivedMessage == null) {
                        // Si aucun message trouvé, attendre une notification d'un nouveau message
                        mailBox.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Error while waiting for message from " + fromProcessName, e);
                }
            }

            // Une fois le message trouvé, on le retire de la boîte aux lettres
            mailBox.remove(receivedMessage);
        }

        logger.info("Synchronous message from " + fromProcessName + " received.");
        return receivedMessage;
    }



    /**
     * Event handler for synchronization messages. When a synchronization message is received from
     * another process, this method adds the sender to the list of synchronized processes and notifies
     * waiting threads if all processes have synchronized.
     *
     * @param syncMessage The synchronization message received.
     */
    @Subscribe
    private void onSync(SynchronizedMessage syncMessage){
        if(syncMessage.getSender().equalsIgnoreCase(this.name)) return;
        synchronized (syncReceived){
            syncReceived.add(syncMessage.getSender());
            this.logger.info("Received synchronization message from " + syncMessage.getSender());
            if(syncReceived.size() == Communicator.maxNbProcess - 1){
                syncReceived.notifyAll();
            }
        }
    }


    /**
     * Event handler for broadcast messages. This method is triggered when a broadcast message is received.
     * It updates the Lamport clock using the message's timestamp, adds the message to the mailbox,
     * and logs the receipt of the message.
     *
     * @param message The broadcast message received.
     */
    @Subscribe
    private void onBroadcast(BroadcastMessage<?> message) {
        clock.update(message.getTimestamp());
        if (message.getSender().equalsIgnoreCase(this.name)) return;
        this.mailBox.add(message);
        this.logger.info("Receiving broadcast message: " + message.getMessage() + " from " + message.getSender());
    }

    /**
     * Event handler for dedicated (point-to-point) messages. When a message is received for the current
     * process, this method updates the Lamport clock, adds the message to the mailbox, and logs the receipt.
     *
     * @param message The dedicated message received.
     */
    @Subscribe
    private void onReceive(DedicatedMessage<?> message) {
        clock.update(message.getTimestamp());
        if (!message.getReceiver().equalsIgnoreCase(this.name)) return;
        this.mailBox.add(message);
        this.logger.info("Receiving message: " + message.getMessage() + " from " + message.getSender());
    }

    /**
     * Event handler for token messages. This method is triggered when a token is received by the current
     * process. If the process is in the "REQUEST" state, it enters the critical section and waits
     * until it is ready to release the token. Once released, the token is passed to the next process.
     *
     * @param tokenMessage The token message received.
     * @throws InterruptedException If the thread is interrupted while waiting to release the token.
     */
    @Subscribe
    private void onToken(TokenMessage<?> tokenMessage) throws InterruptedException {

        //clock.update(tokenMessage.getTimestamp());
        if (!tokenMessage.getToken().getHolder().equalsIgnoreCase(this.name)) return;

        this.logger.info("Received the token");

        if (this.state == TokenState.REQUEST) {
            this.state = TokenState.CRITICAL_SECTION;
            while (this.state != TokenState.RELEASE) {
                Thread.sleep(500);
            }
            this.logger.info("Releasing the token");
        }

        this.state = TokenState.NULL;
        sendTokenToNextProcess(tokenMessage);
    }

    /**
     * Initializes the token by assigning it to the current process and sending it to the next process
     * in the ring. This method is used to start the token ring algorithm for managing the critical section.
     */
    public void initToken(){
        Token token = new Token();
        token.setHolder(this.name);
        TokenMessage<?> tokenMessage = new TokenMessage<>(token, this.name);
        sendTokenToNextProcess(tokenMessage);
    }

    /**
     * Requests access to the critical section. This method sets the process state to "REQUEST" and blocks
     * until the token is received, allowing the process to enter the critical section.
     *
     * @throws InterruptedException If the thread is interrupted while waiting for the token.
     */
    public void requestSC() throws InterruptedException {
        this.state = TokenState.REQUEST;
        while (this.state == TokenState.REQUEST) {
            Thread.sleep(500);
        }
    }

    /**
     * Releases the token after the process has finished its critical section. This method sets the
     * process state to "RELEASE", allowing the token to be passed to the next process.
     */
    public void releaseSC() {
        this.state = TokenState.RELEASE;
    }

}
