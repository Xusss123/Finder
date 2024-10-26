package karm.van.exception.other;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "invalid token or expired")
public class TokenNotExistException extends Exception{

    public TokenNotExistException(String message){
        super(message);
    }

}
