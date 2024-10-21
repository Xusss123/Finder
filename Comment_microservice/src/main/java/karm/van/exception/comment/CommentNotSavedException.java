package karm.van.exception.comment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class CommentNotSavedException extends Exception{

    public CommentNotSavedException(String message){
        super(message);
    }

}
