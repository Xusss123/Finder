package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "You have provided more than 5 images")
public class ImageLimitException extends Exception{

    public ImageLimitException(String message){
        super(message);
    }

}
