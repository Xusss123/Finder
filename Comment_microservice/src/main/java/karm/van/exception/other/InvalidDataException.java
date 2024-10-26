package karm.van.exception.other;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDataException extends Exception{

    public InvalidDataException(String message){
        super(message);
    }


}
