package karm.van.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import karm.van.exception.ImageNotDeletedException;
import karm.van.exception.ImageNotFoundException;
import karm.van.exception.ImageNotSavedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Service
@Slf4j
public class MinioService {

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.user}")
    private String minioUser;

    @Value("${minio.password}")
    private String minioPassword;

    private S3Client client;

    @PostConstruct
    public void MinioServer(){
        client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(minioUser,minioPassword)))
                .endpointOverride(URI.create("http://"+minioEndpoint+":9000"))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    public void moveObject(String oldBucketName, String newBucketName, String fileName) throws ImageNotFoundException {
        try {

            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(oldBucketName)
                    .sourceKey(fileName)
                    .destinationBucket(newBucketName)
                    .destinationKey(fileName)
                    .build();
            client.copyObject(copyObjectRequest);


            delObject(oldBucketName, fileName);

        } catch (NoSuchKeyException e) {
            throw new ImageNotFoundException("The image was not found in the specified bucket: " + oldBucketName);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while moving the image: " + e.getMessage(), e);
        }
    }

    public void putObject(String bucketName, MultipartFile file, String fileName) throws ImageNotSavedException {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }catch (Exception e){
            throw new ImageNotSavedException("An error occurred while uploading the image");
        }
    }

    public void delObject(String bucketName, String imageName) throws ImageNotDeletedException {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageName)
                    .build();
            client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new ImageNotDeletedException("An error occurred while delete the image");
        }
    }

    @PreDestroy
    public void closeMinioClient() {
        if (client != null) {
            client.close();
            log.info("S3Client closed successfully.");
        }
    }
}
