package karm.van.exception.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,reason = "an error occurred while getting the image url")
public class ImageUrlNotGetted extends Exception{

    public ImageUrlNotGetted(String message){
        super(message);
    }
}
