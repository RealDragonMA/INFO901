package fr.usmb.messages;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Message<T> {

    private final T message;
    private int timestamp;
    private String sender;

    public Message(T message, String sender) {
        this.message = message;
        this.timestamp = 0;
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "[Timestamp: " + timestamp + ", Message: " + message + "]";
    }
}
