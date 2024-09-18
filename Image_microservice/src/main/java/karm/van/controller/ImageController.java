package karm.van.controller;

import karm.van.dto.ImageDto;
import karm.van.exception.ImageLimitException;
import karm.van.exception.ImageNotDeletedException;
import karm.van.exception.ImageNotFoundException;
import karm.van.exception.ImageNotSavedException;
import karm.van.model.ImageModel;
import karm.van.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/image")
public class ImageController {
    private final ImageService imageService;

    @GetMapping("/get")
    public List<ImageDto> getCardImages(@RequestParam List<Long> imagesId) {
        return imageService.getImages(imagesId);
    }

    @DeleteMapping("/del/{cardId}/{imageId}")
    public void deleteOneImage(@PathVariable Long cardId, @PathVariable Long imageId) throws ImageNotFoundException, ImageNotDeletedException {
        try {
            imageService.deleteImageFromCard(cardId, imageId);
        } catch (ImageNotDeletedException | ImageNotFoundException e) {
            log.error("Error deleting image: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/addCardImages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> addCardImages(@RequestPart("files") List<MultipartFile> files,
                                    @RequestPart("currentCardImagesCount") int currentCardImagesCount) throws ImageNotSavedException, ImageLimitException {
        try {
            return imageService.addCardImages(files, currentCardImagesCount);
        } catch (ImageNotSavedException | ImageLimitException e) {
            log.error("Error adding card images: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/minio/del")
    public void delImagesFromMinio(@RequestParam List<Long> ids) {
        try {
            imageService.deleteAllImagesFromCard(ids);
        } catch (Exception e) {
            log.error("Error deleting images from Minio: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete images from Minio", e);
        }
    }
}