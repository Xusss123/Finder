package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class CardNotDeletedException extends Exception{

    public CardNotDeletedException(String message){
        super(message);
    }

    public CardNotDeletedException(){
        super();
    }
}
