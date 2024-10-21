package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class CardsNotGetedException extends Exception{

    public CardsNotGetedException(String message){
        super(message);
    }

    public CardsNotGetedException(){
        super();
    }
}
