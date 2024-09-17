package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.config.AdsMicroServiceProperties;
import karm.van.exception.ImageLimitException;
import karm.van.exception.ImageNotDeletedException;
import karm.van.exception.ImageNotFoundException;
import karm.van.exception.ImageNotSavedException;
import karm.van.model.ImageModel;
import karm.van.repository.ImageRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final MinioService minioService;
    private final ImageRepo imageRepo;
    private final AdsMicroServiceProperties adsProperties;
    private WebClient webClient;

    @Value("${minio.bucketNames.image-bucket}")
    private String minioImageBucket;

    @Value("${card.images.count}")
    private int allowedImagesCount;

    @PostConstruct
    public void init(){
        webClient = WebClient.create();
    }

    private void saveImage(MultipartFile file, String fileName) throws ImageNotSavedException {
        try {
            minioService.putObject(minioImageBucket,file, fileName);
        }catch (Exception e){
            throw new ImageNotSavedException(e.getMessage());
        }
    }

    private void delImage(String imageBucket, String imageName) throws ImageNotDeletedException {
        try {
            minioService.delObject(imageBucket,imageName);
        }catch (Exception e){
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
        imagesId.parallelStream().forEach(imageRepo::deleteById);
        deleteImagesFromMinio(imagesId);
    }

    @Transactional
    public void deleteImageFromCard(Long cardId, Long imageId) throws ImageNotFoundException, ImageNotDeletedException {
        ImageModel imageModel = imageRepo.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException("Image with this id doesn't exist"));

        try {
            String url = buildUrl(
                    adsProperties.getPrefix(),
                    adsProperties.getHost(),
                    adsProperties.getPort(),
                    adsProperties.getEndpoints().getDelImage(),
                    cardId,
                    imageId);

            HttpStatusCode statusCode = requestToDelOneImage(url);

            if (statusCode == HttpStatus.OK) {
                minioService.delObject(minioImageBucket, imageModel.getImageName());
                imageRepo.delete(imageModel);
            }
        } catch (ImageNotDeletedException e) {
            throw new ImageNotDeletedException(e.getMessage());
        }
    }

    private String buildUrl(String prefix, String host, String port, String endpoint, Long cardId, Long imageId) {
        return UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint + cardId + "/" + imageId)
                .toUriString();
    }

    private HttpStatusCode requestToDelOneImage(String url) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .retrieve()
                                .toBodilessEntity()
                                .block()
                )
                .getStatusCode();
    }
}
