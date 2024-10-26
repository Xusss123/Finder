package karm.van.exception.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,reason = "an error occurred while uploading the image")
public class ImageNotSavedException extends Exception{

    public ImageNotSavedException(String message){
        super(message);
    }

}
