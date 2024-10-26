package karm.van.exception.token;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "invalid token or expired")
public class InvalidApiKeyException extends Exception{

    public InvalidApiKeyException(String message){
        super(message);
    }

}
