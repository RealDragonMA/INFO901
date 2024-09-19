package fr.usmb.messages;

import lombok.Getter;

@Getter
public class SynchronizedMessage extends Message<String> {

    private final String from;

    public SynchronizedMessage(String from){
        super("Synchronized message");
        this.from = from;
    }
}
