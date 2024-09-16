package karm.van.controller;

import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.image.ImageNotDeletedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.image.ImageNotFoundException;
import karm.van.exception.other.SerializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandlerController {


    @ExceptionHandler(CardNotSavedException.class)
    public ResponseEntity<String> cardNotSavedException(CardNotSavedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ImageLimitException.class)
    public ResponseEntity<String> imageLimitException(ImageLimitException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SerializationException.class)
    public ResponseEntity<String> serializationException(SerializationException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<String> cardNotFoundException(CardNotFoundException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<String> imageNotFoundException(ImageNotFoundException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ImageNotDeletedException.class)
    public ResponseEntity<String> imageDeleteException(ImageNotDeletedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ImageNotSavedException.class)
    public ResponseEntity<String> imageLoadException(ImageNotSavedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
