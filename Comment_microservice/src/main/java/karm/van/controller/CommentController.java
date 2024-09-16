package karm.van.controller;

import karm.van.exception.CardNotFoundException;
import karm.van.exception.CommentNotFoundException;
import karm.van.exception.InvalidDataException;
import karm.van.exception.SerializationException;
import karm.van.model.CommentModel;
import karm.van.service.CommentService;
import karm.van.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/add/{cardId}")
    public void addComment(@PathVariable Long cardId, @RequestPart("commentDto") CommentDto commentDto) throws InvalidDataException, CardNotFoundException {
        try {
            commentService.addComment(cardId,commentDto);
        } catch (InvalidDataException e) {
            throw new InvalidDataException(e.getMessage());
        } catch (CardNotFoundException e) {
            throw new CardNotFoundException(e.getMessage());
        }
    }

    @GetMapping("/get/{cardId}")
    public List<CommentModel> getComment(@PathVariable Long cardId,
                                         @RequestParam(required = false,defaultValue = "0") int page,
                                         @RequestParam(required = false,defaultValue = "10") int limit) throws CardNotFoundException, SerializationException {
        try {
            return commentService.getComments(cardId,limit,page);
        } catch (CardNotFoundException e) {
            throw new CardNotFoundException(e.getMessage());
        } catch (SerializationException e){
            throw new SerializationException(e.getMessage());
        }
    }

    @DeleteMapping("/delAll/{cardId}")
    public void deleteCommentsByCard(@PathVariable Long cardId){
        commentService.deleteAllCommentsByCard(cardId);
    }

    @PatchMapping("/{commentId}/patch")
    public void patchComment(@PathVariable Long commentId,@RequestPart("commentDto") CommentDto commentDto) throws InvalidDataException, CommentNotFoundException {
        try {
            commentService.patchComment(commentId,commentDto);
        }catch (InvalidDataException e){
            throw new InvalidDataException(e.getMessage());
        }catch (CommentNotFoundException e){
            throw new CommentNotFoundException(e.getMessage());
        }
    }
}
