package karm.van.controller;

import karm.van.dto.ImageDto;
import karm.van.exception.*;
import karm.van.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/image")
public class ImageController {
    private final ImageService imageService;

    @Value("${minio.bucketNames.image-bucket}")
    private String minioImageBucket;

    @Value("${minio.bucketNames.profile-image-bucket}")
    private String minioProfileImageBucket;

    @Value("${minio.bucketNames.trash-bucket}")
    private String minioTrashBucket;

    @GetMapping("/get")
    public List<ImageDto> getCardImages(@RequestParam List<Long> imagesId,
                                        @RequestHeader("x-api-key") String key,
                                        @RequestHeader("Authorization") String authorization) throws TokenNotExistException, InvalidApiKeyException {
        if(imageService.checkNoneEqualsApiKey(key)){
            throw new InvalidApiKeyException("Invalid api-key");
        }
        return imageService.getImages(imagesId,authorization);
    }

    @GetMapping("/get-one/{imageId}")
    public ResponseEntity<?> getImage(@PathVariable Long imageId,
                                      @RequestHeader("Authorization") String authorization,
                                      @RequestHeader("x-api-key") String key){
        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }

            return ResponseEntity.ok(imageService.getImage(imageId,authorization));
        } catch (ImageNotFoundException | TokenNotExistException | InvalidApiKeyException e) {
            log.error("Error deleting image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/del/{imageId}")
    public ResponseEntity<?> deleteOneImage(@PathVariable Long imageId,
                               @RequestHeader("Authorization") String authorization,
                               @RequestHeader("x-api-key") String key){

        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            imageService.deleteImage(imageId,authorization);
            return ResponseEntity.ok("Success delete");
        } catch (ImageNotFoundException | TokenNotExistException | InvalidApiKeyException e) {
            log.error("Error deleting image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ImageNotDeletedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping(value = "/addCardImages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> addCardImages(@RequestPart("files") List<MultipartFile> files,
                                    @RequestPart("currentCardImagesCount") int currentCardImagesCount,
                                    @RequestHeader("Authorization") String authorization,
                                    @RequestHeader("x-api-key") String key) throws ImageNotSavedException, ImageLimitException, TokenNotExistException, InvalidApiKeyException {
        if(imageService.checkNoneEqualsApiKey(key)){
            throw new InvalidApiKeyException("Invalid api-key");
        }
        try {
            return imageService.addCardImages(files, currentCardImagesCount,authorization,minioImageBucket);
        } catch (ImageNotSavedException | ImageLimitException | TokenNotExistException e) {
            log.error("Error adding card images: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/addProfileImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProfileImage(@RequestPart("profileImage") MultipartFile profileImage,
                                    @RequestHeader("Authorization") String authorization) {
        try {
            imageService.addProfileImage(profileImage,authorization,minioProfileImageBucket);
            return ResponseEntity.ok("The profile picture has been successfully added");
        } catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid token");
        } catch (ImageNotSavedException e) {
            log.error("Error adding profile image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding images");
        }
    }

    @PostMapping(value = "/move")
    public ResponseEntity<?> moveImagesBetweenBuckets(@RequestParam List<Long> ids,
                                                      @RequestParam(value = "toTrash",required = false,defaultValue = "false") Boolean toTrash,
                                                      @RequestHeader("Authorization") String authorization,
                                                      @RequestHeader("x-api-key") String key) {



        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }

            String bucketToMove = toTrash? minioTrashBucket:minioImageBucket;

            imageService.moveAllImagesBetweenBuckets(ids,authorization,bucketToMove);
            return ResponseEntity.ok("ok");
        } catch (TokenNotExistException | InvalidApiKeyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error moving images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error moving images");
        }
    }

    @PostMapping(value = "/profile/move/{imageId}")
    public ResponseEntity<?> moveProfileImagesBetweenBuckets(@PathVariable Long imageId,
                                                             @RequestHeader("Authorization") String authorization,
                                                             @RequestParam(value = "toTrash",required = false,defaultValue = "false") Boolean toTrash,
                                                             @RequestHeader("x-api-key") String key) {



        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            String bucketToMove = toTrash? minioTrashBucket:minioProfileImageBucket;

            imageService.moveImageBetweenBuckets(imageId,authorization,bucketToMove);
            return ResponseEntity.ok("ok");
        } catch (TokenNotExistException | InvalidApiKeyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error moving images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error moving images");
        }
    }

    @DeleteMapping("/minio/del")
    public ResponseEntity<?> delImagesFromMinio(@RequestParam List<Long> ids,
                                                @RequestHeader("Authorization") String authorization,
                                                @RequestHeader("x-api-key") String key) {


        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }

            imageService.deleteAllImages(ids,authorization);
            return ResponseEntity.ok("Images deleted");
        } catch (TokenNotExistException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e) {
            log.error("Error deleting images from Minio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete images from Minio");
        }
    }
}