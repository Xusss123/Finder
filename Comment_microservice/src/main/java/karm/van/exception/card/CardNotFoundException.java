package karm.van.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND,reason = "card with this id doesn't exist")
public class CardNotFoundException extends Exception{

    public CardNotFoundException(String message){
        super(message);
    }

    public CardNotFoundException(){
        super();
    }
}
