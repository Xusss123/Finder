package karm.van.exception.other;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "An error occurred on the server")
public class ServerException extends Exception{

    public ServerException(String message){
        super(message);
    }

    public ServerException(){
        super();
    }
}
