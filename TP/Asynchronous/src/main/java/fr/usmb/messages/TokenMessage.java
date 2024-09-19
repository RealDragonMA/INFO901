package fr.usmb.messages;

import fr.usmb.token.Token;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenMessage<T> extends Message<T>{

    private Token token;

    public TokenMessage(Token token) {
        super(null);
        this.token = token;
    }
}
