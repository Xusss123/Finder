package karm.van.exception.comment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class CommentNotDeletedException extends Exception{

    public CommentNotDeletedException(String message){
        super(message);
    }

    public CommentNotDeletedException(){
        super();
    }
}
