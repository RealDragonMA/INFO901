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

    public void add(Message<?> message){
        this.mailBox.add(message);
    }

    public void remove(Message<?> message){
        this.mailBox.remove(message);
    }

    public void removeAtIndex(int index){
        this.mailBox.remove(index);
    }

    public <T> Message<T> getMessage(){
        Message<T> message = (Message<T>) this.mailBox.get(0);
        this.removeAtIndex(0);
        return message;
    }

    public List<Message<?>> getMessages(){
        return this.mailBox;
    }

    public boolean isEmpty(){
        return this.mailBox.isEmpty();
    }

}
