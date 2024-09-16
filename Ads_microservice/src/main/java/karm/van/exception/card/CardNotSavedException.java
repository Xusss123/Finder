package karm.van.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "invalid card data")
public class CardNotSavedException extends Exception{
    public CardNotSavedException(String message){
        super(message);
    }

    public CardNotSavedException(){

    }
}
