package karm.van.exception.comment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND,reason = "comment with this id doesn't exist")
public class CommentNotFoundException extends Exception{

    public CommentNotFoundException(String message){
        super(message);
    }

}
