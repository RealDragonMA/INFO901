package fr.usmb.messages;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BroadcastMessage<T> extends Message<T>{

    private String sender;

    public BroadcastMessage(T message) {
        super(message);
    }
}
