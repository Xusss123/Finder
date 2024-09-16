package karm.van.service;

import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.image.ImageNotDeletedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.image.ImageNotFoundException;
import karm.van.model.CardModel;
import karm.van.model.ImageModel;
import karm.van.repository.ImageRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final MinioService minioService;
    private final ImageRepo imageRepo;

    @Value("${minio.bucketNames.image-bucket}")
    private String minioImageBucket;

    @Value("${card.images.count}")
    private int allowedImagesCount;

    private void saveImage(MultipartFile file, String fileName) throws ImageNotSavedException {
        try {
            minioService.putObject(minioImageBucket,file, fileName);
        }catch (Exception e){
            throw new ImageNotSavedException(e.getMessage());
        }
    }

    private void delImage(String imageBucket, String imageName) throws ImageNotSavedException {
        try {
            minioService.delObject(imageBucket,imageName);
        }catch (Exception e){
            throw new ImageNotSavedException(e.getMessage());
        }
    }

    @Transactional
    public void addCardImages(List<MultipartFile> files, CardModel cardModel, int currentCardImagesCount) throws ImageNotSavedException, ImageLimitException, CardNotSavedException {

        if (currentCardImagesCount<allowedImagesCount){
            int freeMemory = allowedImagesCount - currentCardImagesCount;

            List<MultipartFile> filesToAdd = files.subList(0,Math.min(freeMemory,files.size()));

            String unique_uuid = UUID.randomUUID().toString();

            for (MultipartFile file:filesToAdd){
                String fileName = unique_uuid+"-"+file.getOriginalFilename();

                ImageModel imageModel = ImageModel.builder()
                        .imageName(fileName)
                        .imageBucket(minioImageBucket)
                        .card(cardModel)
                        .build();
                try {
                    imageRepo.save(imageModel);
                }catch (Exception e){
                    throw new CardNotSavedException("There was a problem while saving the article");
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
        }
    }

    @Transactional
    public void deleteImagesFromMinio(CardModel cardModel){
        cardModel.getImages().parallelStream().forEach(image -> {
            try {
                delImage(image.getImageBucket(), image.getImageName());
            } catch (ImageNotSavedException e) {
                throw new RuntimeException(new ImageNotDeletedException("An error occurred while deleting the card"));
            }
        });
    }

    @Transactional
    public void deleteImageFromCard(Long id) throws ImageNotFoundException, ImageNotDeletedException {
        ImageModel imageModel = imageRepo.findById(id)
                .orElseThrow(()->new ImageNotFoundException("Image with this id doesn't exist"));

        try {
            minioService.delObject(minioImageBucket,imageModel.getImageName());
            imageRepo.delete(imageModel);
        } catch (ImageNotDeletedException e) {
            throw new ImageNotDeletedException(e.getMessage());
        }

    }
}
