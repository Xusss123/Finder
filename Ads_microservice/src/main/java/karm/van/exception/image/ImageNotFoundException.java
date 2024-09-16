package karm.van.exception.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND,reason = "Image with this id doesn't exits")
public class ImageNotFoundException extends Exception{

    public ImageNotFoundException(String message){
        super(message);
    }

    public ImageNotFoundException(){
        super();
    }
}
