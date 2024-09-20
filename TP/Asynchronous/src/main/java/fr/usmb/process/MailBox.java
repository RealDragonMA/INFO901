package fr.usmb.process;

import fr.usmb.messages.Message;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class MailBox {

    private final List<Message<?>> mailBox;

    public MailBox(){
        this.mailBox = new ArrayList<>();
    }

    /**
     * Adds a message to the mailbox. This method allows messages to be queued
     * and retrieved by the process at a later time.
     *
     * @param message The message to add to the mailbox.
     */
    public void add(Message<?> message){
        this.mailBox.add(message);
    }

    /**
     * Removes a specific message from the mailbox. This method is used to
     * remove a message once it has been processed by the process.
     *
     * @param message The message to remove from the mailbox.
     */
    public void remove(Message<?> message){
        this.mailBox.remove(message);
    }

    /**
     * Removes a message from the mailbox at a specific index. This method is useful
     * when the process needs to remove a message based on its position in the queue.
     *
     * @param index The index of the message to remove.
     */
    public void removeAtIndex(int index){
        this.mailBox.remove(index);
    }

    /**
     * Retrieves and removes the first message in the mailbox. This method returns
     * the message at the front of the queue, and removes it from the mailbox.
     *
     * @param <T> The type of the message payload.
     * @return The first message in the mailbox.
     * @throws IndexOutOfBoundsException if the mailbox is empty.
     */
    public <T> Message<T> getMessage(){
        Message<T> message = (Message<T>) this.mailBox.get(0);
        this.removeAtIndex(0);
        return message;
    }

    /**
     * Retrieves all messages currently in the mailbox. This method returns
     * a list of all messages without removing them from the mailbox.
     *
     * @return A list of all messages in the mailbox.
     */
    public List<Message<?>> getMessages(){
        return this.mailBox;
    }

    /**
     * Checks whether the mailbox is empty. This method returns true if there are
     * no messages in the mailbox, and false otherwise.
     *
     * @return True if the mailbox is empty, false if it contains messages.
     */
    public boolean isEmpty(){
        return this.mailBox.isEmpty();
    }

}
