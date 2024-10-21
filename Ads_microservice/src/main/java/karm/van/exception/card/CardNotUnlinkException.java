package karm.van.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CardNotUnlinkException extends Exception{

    public CardNotUnlinkException(String message){
        super(message);
    }

}
