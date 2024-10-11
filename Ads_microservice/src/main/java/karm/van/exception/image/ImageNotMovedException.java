package karm.van.exception.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,reason = "an error occurred while delete the image")
public class ImageNotMovedException extends Exception{

    public ImageNotMovedException(String message){
        super(message);
    }

    public ImageNotMovedException(){
        super();
    }
}
