package karm.van.controller;

import karm.van.exception.CardNotFoundException;
import karm.van.exception.CommentNotFoundException;
import karm.van.exception.InvalidDataException;
import karm.van.exception.SerializationException;
import karm.van.model.CommentModel;
import karm.van.service.CommentService;
import karm.van.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/add/{cardId}")
    public void addComment(@PathVariable Long cardId, @RequestPart("commentDto") CommentDto commentDto) throws InvalidDataException, CardNotFoundException {
        try {
            commentService.addComment(cardId, commentDto);
        } catch (InvalidDataException | CardNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @GetMapping("/get/{cardId}")
    public List<CommentModel> getComment(@PathVariable Long cardId,
                                         @RequestParam(required = false,defaultValue = "0") int page,
                                         @RequestParam(required = false,defaultValue = "10") int limit) throws CardNotFoundException, SerializationException {
        try {
            return commentService.getComments(cardId,limit,page);
        } catch (CardNotFoundException | SerializationException e) {
            throw e;
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @DeleteMapping("/delAll/{cardId}")
    public void deleteCommentsByCard(@PathVariable Long cardId){
        try {
            commentService.deleteAllCommentsByCard(cardId);
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @DeleteMapping("/del/{commentId}")
    public void deleteOneComment(@PathVariable Long commentId){
        try {
            commentService.deleteOneComment(commentId);
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @PatchMapping("/{commentId}/patch")
    public void patchComment(@PathVariable Long commentId,@RequestPart("commentDto") CommentDto commentDto) throws InvalidDataException, CommentNotFoundException {
        try {
            commentService.patchComment(commentId, commentDto);
        }catch (InvalidDataException | CommentNotFoundException e){
            throw e;
        }catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }
}
