package karm.van.controller;

import karm.van.exception.ImageLimitException;
import karm.van.exception.ImageNotDeletedException;
import karm.van.exception.ImageNotFoundException;
import karm.van.exception.ImageNotSavedException;
import karm.van.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/image/")
public class ImageController {
    private final ImageService imageService;

    @DeleteMapping("/del/{cardId}/{imageId}")
    public void deleteOneImage(@PathVariable Long cardId,@PathVariable Long imageId) throws ImageNotDeletedException, ImageNotFoundException {
        try {
            imageService.deleteImageFromCard(cardId,imageId);
        }catch (ImageNotDeletedException e){
            throw new ImageNotDeletedException(e.getMessage());
        }catch (ImageNotFoundException e){
            throw new ImageNotFoundException(e.getMessage());
        }
    }

    @PostMapping(value = "addCardImages",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> addCardImages(@RequestPart("files") List<MultipartFile> files,
                                    @RequestPart("currentCardImagesCount") int currentCardImagesCount) throws ImageNotSavedException, ImageLimitException {
        try {
            return imageService.addCardImages(files,currentCardImagesCount);
        } catch (ImageNotSavedException e) {
            throw new ImageNotSavedException(e.getMessage());
        } catch (ImageLimitException e) {
            throw new ImageLimitException(e.getMessage());
        }
    }

    @DeleteMapping("minio/del")
    public void delImagesFromMinio(@RequestParam("ids") List<Long> listOfImagesId){
        try {
            imageService.deleteAllImagesFromCard(listOfImagesId);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
}
