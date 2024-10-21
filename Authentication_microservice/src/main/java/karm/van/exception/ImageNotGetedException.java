package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ImageNotGetedException extends Exception{

    public ImageNotGetedException(String message){
        super(message);
    }

    public ImageNotGetedException(){
        super();
    }
}
