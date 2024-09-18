package fr.usmb.messages;

public class SynchronizedMessage extends Message<String> {

    public enum Type {
        REQUEST,
        COMPLETE
    }

    public SynchronizedMessage(Type type) {
        super(type.toString());
    }
}
