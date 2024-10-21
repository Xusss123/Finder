package karm.van.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CardNotDeletedException extends Exception{

    public CardNotDeletedException(String message){
        super(message);
    }
}
