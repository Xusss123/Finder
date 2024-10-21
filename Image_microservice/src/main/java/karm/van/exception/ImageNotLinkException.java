package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,reason = "an error occurred while linking the image")
public class ImageNotLinkException extends Exception{

    public ImageNotLinkException(String message){
        super(message);
    }

}
