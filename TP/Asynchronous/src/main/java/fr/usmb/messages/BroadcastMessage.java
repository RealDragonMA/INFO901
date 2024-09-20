package fr.usmb.messages;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BroadcastMessage<T> extends Message<T>{

    public BroadcastMessage(T message, String sender) {
        super(message, sender);
    }
}
