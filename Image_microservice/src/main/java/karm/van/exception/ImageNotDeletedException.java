package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,reason = "an error occurred while delete the image")
public class ImageNotDeletedException extends Exception{

    public ImageNotDeletedException(String message){
        super(message);
    }

}
