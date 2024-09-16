package karm.van.controller;

import karm.van.exception.CardNotFoundException;
import karm.van.exception.CommentNotFoundException;
import karm.van.exception.InvalidDataException;
import karm.van.exception.SerializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandlerController {


    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<String> cardNotSavedException(CardNotFoundException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<String> commentNotFoundException(CommentNotFoundException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<String> serializationException(InvalidDataException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SerializationException.class)
    public ResponseEntity<String> serializationException(SerializationException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }


}
