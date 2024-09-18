package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.config.AdsMicroServiceProperties;
import karm.van.dto.ImageDto;
import karm.van.exception.ImageLimitException;
import karm.van.exception.ImageNotDeletedException;
import karm.van.exception.ImageNotFoundException;
import karm.van.exception.ImageNotSavedException;
import karm.van.model.ImageModel;
import karm.van.repository.ImageRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class ImageService {
    private final MinioService minioService;
    private final ImageRepo imageRepo;
    private final AdsMicroServiceProperties adsProperties;
    private final RestService restService;

    @Value("${minio.bucketNames.image-bucket}")
    private String minioImageBucket;

    @Value("${card.images.count}")
    private int allowedImagesCount;

    private void saveImage(MultipartFile file, String fileName) throws ImageNotSavedException {
        try {
            minioService.putObject(minioImageBucket,file, fileName);
        }catch (Exception e){
            log.error("The image was not saved: "+e.getMessage()+" - "+e.getClass());
            throw new ImageNotSavedException(e.getMessage());
        }
    }

    private void delImage(String imageBucket, String imageName) throws ImageNotDeletedException {
        try {
            minioService.delObject(imageBucket,imageName);
        }catch (Exception e){
            log.error("The image was not deleted: "+e.getMessage()+" - "+e.getClass());
            throw new ImageNotDeletedException(e.getMessage());
        }
    }

    @Transactional
    public List<Long> addCardImages(List<MultipartFile> files, int currentCardImagesCount) throws ImageNotSavedException, ImageLimitException{
        if (currentCardImagesCount<allowedImagesCount){
            int freeMemory = allowedImagesCount - currentCardImagesCount;

            List<MultipartFile> filesToAdd = files.subList(0,Math.min(freeMemory,files.size()));

            String unique_uuid = UUID.randomUUID().toString();

            List<Long> imagesId = new ArrayList<>();

            for (MultipartFile file:filesToAdd){
                String fileName = unique_uuid+"-"+file.getOriginalFilename();

                ImageModel imageModel = ImageModel.builder()
                        .imageName(fileName)
                        .imageBucket(minioImageBucket)
                        .build();
                try {
                    imageRepo.save(imageModel);
                    imagesId.add(imageModel.getId());
                }catch (Exception e){
                    throw new ImageNotSavedException("There was a problem while saving the image");
                }
            }

            filesToAdd.parallelStream().forEach(file -> {
                String fileName = unique_uuid+"-"+file.getOriginalFilename();

                try {
                    saveImage(file,fileName);
                }catch (Exception e){
                    throw new RuntimeException(new ImageNotSavedException("There is a problem with image processing, so the article has not been published"));
                }
            });
            return imagesId;
        }else {
            throw new ImageLimitException("You have provided more than" + allowedImagesCount + "images");
        }
    }

    private void deleteImagesFromMinio(List<Long> imagesId){
        imagesId.parallelStream().forEach(imageId->
                imageRepo.findById(imageId).ifPresent(image->{
            try {
                delImage(image.getImageBucket(), image.getImageName());
            } catch (ImageNotDeletedException e) {
                throw new RuntimeException(new ImageNotDeletedException("An error occurred while deleting the card"));
            }
        }));
    }

    @Transactional
    public void deleteAllImagesFromCard(List<Long> imagesId) {
        deleteImagesFromMinio(imagesId);
        imageRepo.deleteAllById(imagesId);
        log.debug(imagesId.toString());
    }

    @Transactional
    public void deleteImageFromCard(Long cardId, Long imageId) throws ImageNotFoundException, ImageNotDeletedException {
        ImageModel imageModel = imageRepo.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException("Image with this id doesn't exist"));

        String url = restService.buildUrl(
                adsProperties.getPrefix(),
                adsProperties.getHost(),
                adsProperties.getPort(),
                adsProperties.getEndpoints().getDelImage(),
                cardId,
                imageId);

        HttpStatusCode statusCode = restService.requestToDelOneImage(url);

        if (statusCode == HttpStatus.OK) {
            minioService.delObject(minioImageBucket, imageModel.getImageName());
            imageRepo.delete(imageModel);
        } else {
            throw new ImageNotDeletedException("Failed to delete image");
        }
    }

    private ImageDto imageModelToDto(ImageModel imageModel){
        return new ImageDto(imageModel.getId(),imageModel.getImageBucket(),imageModel.getImageName());
    }

    public List<ImageDto> getImages(List<Long> imagesId) {
        return imageRepo.findAllById(imagesId)
                .stream().map(this::imageModelToDto).toList();
    }
}
