package karm.van.controller;

import karm.van.exception.ImageLimitException;
import karm.van.exception.ImageNotDeletedException;
import karm.van.exception.ImageNotFoundException;
import karm.van.exception.ImageNotSavedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandlerController {


    @ExceptionHandler(ImageLimitException.class)
    public ResponseEntity<String> imageLimitException(ImageLimitException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
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
