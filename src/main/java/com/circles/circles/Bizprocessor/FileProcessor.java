package com.circles.circles.Bizprocessor;

import java.io.InputStream;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.circles.circles.Model.User;
import com.circles.circles.Model.fileMetadata;
import com.circles.circles.Repository.FileRepo;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FileProcessor {
    
    private final S3Client s3Client;
    private final FileRepo fileRepository;
    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);
    
    @Value("${aws.s3.bucket.name}")
    private String bucket;

    public FileProcessor(S3Client s3Client, FileRepo fileRepository){
        this.s3Client = s3Client;
        this.fileRepository = fileRepository;
    }

    /**
     * Uploads a file to S3 with a naming convention: uploads/circle_{circleId}/{fileName}
     */
    public String uploadFile(String circleId, String fileName, InputStream fileStream, long contentLength , String fileType){
        String key = String.format("uploads/circle_%s/%s", circleId, fileName);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        Long userId = user.getId();

        try {
            logger.info("Uploading file to S3 with key: {}", key);
            s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(),
                RequestBody.fromInputStream(fileStream, contentLength));
            
            //Saving to postgres
            fileMetadata metadata = new fileMetadata();
            metadata.setCircle_id(Long.parseLong(circleId));
            metadata.setS3_key(key);
            metadata.setFile_name(fileName);
            metadata.setFile_size(contentLength);
            metadata.setFile_type(fileType);
            metadata.setUploaded_at(LocalDateTime.now());
            metadata.setUploaded_by(userId);
            
            fileRepository.save(metadata);
            logger.info("File upload completed with details + " + metadata.toString());
            return "Success";
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("AWS Exception occurred while uploading to S3 key: {}", key, e);
            return "Failure";
        }
    }

    public ResponseBytes<GetObjectResponse> downloadFile(String key){
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .key(key)
                    .bucket(bucket)
                    .build()
            );
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Exception occurred while downloading key: {}", key, e);
            return null;
        }
    }

    public String deleteFile(String key){
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .key(key)
                    .bucket(bucket)
                    .build()
            );
            return "Success";
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Exception occurred while deleting key: {}", key, e);
            return "Failure";
        }
    }

}
