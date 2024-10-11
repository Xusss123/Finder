package karm.van.exception.comment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CommentNotUnlinkException extends Exception{

    public CommentNotUnlinkException(String message){
        super(message);
    }

    public CommentNotUnlinkException(){
        super();
    }
}
