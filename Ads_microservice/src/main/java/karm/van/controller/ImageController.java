package karm.van.controller;

import karm.van.exception.image.ImageNotDeletedException;
import karm.van.exception.image.ImageNotFoundException;
import karm.van.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/image/")
public class ImageController {
    private final ImageService imageService;

    @DeleteMapping("{id}/del")
    public void deleteImage(@PathVariable Long id) throws ImageNotDeletedException, ImageNotFoundException {
        try {
            imageService.deleteImageFromCard(id);
        }catch (ImageNotDeletedException e){
            throw new ImageNotDeletedException(e.getMessage());
        }catch (ImageNotFoundException e){
            throw new ImageNotFoundException(e.getMessage());
        }
    }
}
