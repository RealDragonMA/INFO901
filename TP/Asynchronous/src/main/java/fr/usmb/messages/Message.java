package fr.usmb.messages;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Message<T> {

    private final T message;
    private int timestamp;

    public Message(T message) {
        this.message = message;
        this.timestamp = 0;
    }

    @Override
    public String toString() {
        return "[Timestamp: " + timestamp + ", Message: " + message + "]";
    }
}
