package karm.van.controller;

import karm.van.dto.CardDto;
import karm.van.exception.card.CardNotDeletedException;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.image.ImageNotDeletedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.other.SerializationException;
import karm.van.model.CardModel;
import karm.van.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/card/")
public class CardController {
    private final CardService cardService;

    @GetMapping("{id}/get")
    public CardModel getCard(@PathVariable Long id) throws CardNotFoundException, SerializationException {

        try {
            return cardService.getCard(id);
        } catch (SerializationException e) {
            throw new SerializationException(e.getMessage());
        } catch (CardNotFoundException e){
            throw new CardNotFoundException(e.getMessage());
        }

    }

    @GetMapping("getAll")
    public List<CardModel> getAllCards(){
        return cardService.getAllCards();
    }

    @PostMapping(value = "add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void addCard(@RequestPart("cardDto") CardDto cardDto, @RequestPart("files") List<MultipartFile> files) throws ImageNotSavedException, CardNotSavedException, ImageLimitException {
        try {
            cardService.addCard(files, cardDto);
        }catch (ImageNotSavedException e){
            throw new ImageNotSavedException(e.getMessage());
        }catch (CardNotSavedException e){
            throw new CardNotSavedException(e.getMessage());
        }catch (ImageLimitException e){
            throw new ImageLimitException(e.getMessage());
        }
    }

    @PatchMapping(value = "{id}/patch",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void patchCard(@PathVariable Long id,
                          @RequestPart(value = "cardDto",required = false) Optional<CardDto> cardDto,
                          @RequestPart(value = "files",required = false) Optional<List<MultipartFile>> files) throws CardNotFoundException, CardNotSavedException, ImageNotSavedException, ImageNotDeletedException, ImageLimitException {
        try {
            cardService.patchCard(id,cardDto,files);
        } catch (CardNotFoundException e) {
            throw new CardNotFoundException(e.getMessage());
        } catch (CardNotSavedException e) {
            throw new CardNotSavedException(e.getMessage());
        } catch (ImageNotSavedException e) {
            throw new ImageNotSavedException(e.getMessage());
        } catch (ImageNotDeletedException e){
            throw new ImageNotDeletedException(e.getMessage());
        } catch (ImageLimitException e){
            throw new ImageLimitException(e.getMessage());
        }
    }

    @DeleteMapping("{id}/del")
    public void delCard(@PathVariable Long id) throws CardNotDeletedException, CardNotFoundException {
        try {
            cardService.deleteCard(id);
        } catch (CardNotDeletedException e){
            throw new CardNotDeletedException(e.getMessage());
        } catch (CardNotFoundException e){
            throw new CardNotFoundException(e.getMessage());
        }
    }


}
