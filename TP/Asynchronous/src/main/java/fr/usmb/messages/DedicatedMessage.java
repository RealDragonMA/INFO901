package fr.usmb.messages;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DedicatedMessage<T> extends Message<T> {

    private String receiver;

    public DedicatedMessage(T message, String sender, String receiver) {
        super(message, sender);
    }
}
