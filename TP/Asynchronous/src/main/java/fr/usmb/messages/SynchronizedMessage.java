package fr.usmb.messages;

import lombok.Getter;

@Getter
public class SynchronizedMessage extends Message<String> {

    public SynchronizedMessage(String from){
        super("Synchronized message", from);
    }
}
