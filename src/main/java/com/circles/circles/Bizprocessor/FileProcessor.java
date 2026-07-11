package com.circles.circles.Bizprocessor;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.circles.circles.DTO.fileDTO;
import com.circles.circles.Model.User;
import com.circles.circles.Model.fileMetadata;
import com.circles.circles.Repository.CircleRelRepo;
import com.circles.circles.Repository.FileRepo;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class FileProcessor {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepo fileRepository;
    private final CircleRelRepo circleRelRepo;
    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    @Value("${aws.s3.bucket.name}")
    private String bucket;

    @Value("${aws.s3.thumbnail.bucket.name}")
    private String thumbnailBucket;

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

    public FileProcessor(S3Client s3Client, FileRepo fileRepository, CircleRelRepo circleRelRepo,
            S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.fileRepository = fileRepository;
        this.circleRelRepo = circleRelRepo;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Uploads a file to S3 with a UUID-prefixed object key under circles/{circleId}/.
     */
    public String uploadFile(String circleId, String fileName, InputStream fileStream, long contentLength,
            String fileType) {
        Long parsedCircleId = Long.valueOf(circleId);
        Long userId = getAuthenticatedUserId();

        if (!isCircleMember(userId, parsedCircleId)) {
            logger.warn("Unauthorized file upload attempt. userId={}, circleId={}", userId, circleId);
            return "Unauthorized";
        }

        String safeFileName = sanitizeFileName(fileName);
        String key = String.format("circles/%s/%s-%s", circleId, UUID.randomUUID(), safeFileName);

        try {
            logger.info("Uploading file to S3 with key: {}", key);
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(fileType)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .build(),
                    RequestBody.fromInputStream(fileStream, contentLength));

            // Saving to postgres
            fileMetadata metadata = new fileMetadata();
            metadata.setCircle_id(parsedCircleId);
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

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }

        return fileName.replaceAll("[\\\\/]+", "_");
    }

    //1.Make this paginated , and fetch thumbnails for this aswell and store in fileDTO object
    public List<fileDTO> getAllDocs(String circleId , int page , int size) {
        Long parsedCircleId = Long.valueOf(circleId);
        Long userId = getAuthenticatedUserId();

        if (!isCircleMember(userId, parsedCircleId)) {
            throw new SecurityException("User is not a member of this circle");
        }
        Pageable pageable = PageRequest.of(page, size , Sort.by(Sort.Direction.DESC, "uploaded_at"));
        List<fileDTO> files = fileRepository.getAllDocs(parsedCircleId , pageable);

        for(fileDTO file : files){
            if(file.getFileType() != null && file.getFileType().contains("image/")){
                String awsKey = file.getFileS3Key();
                String thumbnailKey = "resized-" + awsKey;
                try {
                    if (s3KeyExists(thumbnailBucket, thumbnailKey)) {
                        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
                        .signatureDuration(DOWNLOAD_URL_TTL)
                        .getObjectRequest(g -> g
                                .bucket(thumbnailBucket)
                                .key(thumbnailKey)));

                        logger.info("Thumbnail presigning done for thumbnailKey: {} " , thumbnailKey);
                        file.setFileURL(presignedRequest.url().toString());
                    } else {
                        logger.warn("Thumbnail does not exist in bucket {} for key: {}", thumbnailBucket, thumbnailKey);
                    }
                } catch(Exception e) {
                    logger.error("Exception occurred while generating presigned key for thumbnailKey: {}", thumbnailKey, e);
                }
            }
            file.setFileS3Key(null);    //Not sending this to frontend
        }

        return files;
    }

    private boolean s3KeyExists(String bucketName, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            logger.warn("NoSuchKeyException: Key {} not found in bucket {}", key, bucketName);
            return false;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 404) {
                logger.warn("S3 404: Key {} not found in bucket {}", key, bucketName);
                return false;
            }
            logger.error("AWS S3 error checking key existence. bucket={}, key={}, status={}", bucketName, key, e.statusCode(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error checking key existence. bucket={}, key={}", bucketName, key, e);
            return false;
        }
    }
    //Wont need this function , we will generate presigned urls while doing getAllDocs to prevent multiple backend calls for essentially the same thing
    // public List<FileDownloadResult> getThumbnail(Long circleId , int page , int size){
    //     //1.Get the s3 keynames
    //     Long userId = getAuthenticatedUserId();
    //     List<FileDownloadResult> thumbnails = null;
    //     if(!isCircleMember(userId, circleId)){
    //         logger.warn("Unauthorized thumbnail attempt, userId={} , circleId={}" , userId,circleId);
    //         thumbnails.add(FileDownloadResult.failure());
    //         return thumbnails;
    //     }
    //     //2.Iterate through and generate the presigned urls
    //     Pageable pageable = PageRequest.of(page, size , Sort.by("uploadedAt").descending());

    //     List<fileMetadata> thumbnailMd = fileRepository.findByCircleIdAndFileType(circleId, pageable);
    //     if(!thumbnailMd.isEmpty()){
    //         for(fileMetadata fm : thumbnailMd){
    //             String awsKey = fm.getS3_key();
    //             //TODO : Add Redis caching of the presigned urls for 15 minute duration
    //             try{
    //                 PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
    //                 .signatureDuration(DOWNLOAD_URL_TTL)
    //                 .getObjectRequest(g -> g
    //                         .bucket(thumbnailBucket)
    //                         .key(awsKey)));

    //                 logger.info("Thumbnail presigning done for awsKey {} " , awsKey);
    //                 FileDownloadResult i = new FileDownloadResult(DownloadStatus.SUCCESS, presignedRequest.url().toString(),null,  DOWNLOAD_URL_TTL.toSeconds());
    //                 thumbnails.add(i);
    //             }catch(Exception e){
    //             logger.error("Exception occurred while creating presigned download URL. circleId={}", circleId , e);
    //             thumbnails.add(FileDownloadResult.failure());
    //             return thumbnails;
    //             }

    //         }
        
    //     }
    //     return thumbnails;


    //     //3.Success
    // }
    public FileDownloadResult createDownloadUrl(String circleId, String fileId) {
        Long parsedCircleId = Long.valueOf(circleId);
        Long parsedFileId = Long.valueOf(fileId);
        Long userId = getAuthenticatedUserId();

        if (!isCircleMember(userId, parsedCircleId)) {
            logger.warn("Unauthorized file download attempt. userId={}, circleId={}, fileId={}", userId, circleId, fileId);
            return FileDownloadResult.unauthorized();
        }

        Optional<fileMetadata> fileDetails = fileRepository.findByIdAndCircleId(parsedFileId, parsedCircleId);
        if (fileDetails.isEmpty()) {
            logger.info("File not found for download. circleId={}, fileId={}", circleId, fileId);
            return FileDownloadResult.notFound();
        }

        fileMetadata fileObj = fileDetails.get();

        try {
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
                    .signatureDuration(DOWNLOAD_URL_TTL)
                    .getObjectRequest(g -> g
                            .bucket(bucket)
                            .key(fileObj.getS3_key())));

            return FileDownloadResult.success(
                    presignedRequest.url().toString(),
                    fileObj.getFile_name(),
                    DOWNLOAD_URL_TTL.toSeconds());
        } catch (Exception e) {
            logger.error("Exception occurred while creating presigned download URL. circleId={}, fileId={}", circleId, fileId, e);
            return FileDownloadResult.failure();
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return user.getId();
    }

    private boolean isCircleMember(Long userId, Long circleId) {
        return circleRelRepo.findByUserIdAndCircleId(userId, circleId).isPresent();
    }

    public record FileDownloadResult(
            DownloadStatus status,
            String downloadUrl,
            String fileName,
            long expiresInSeconds) {

        public static FileDownloadResult success(String downloadUrl, String fileName, long expiresInSeconds) {
            return new FileDownloadResult(DownloadStatus.SUCCESS, downloadUrl, fileName, expiresInSeconds);
        }

        public static FileDownloadResult unauthorized() {
            return new FileDownloadResult(DownloadStatus.UNAUTHORIZED, null, null, 0);
        }

        public static FileDownloadResult notFound() {
            return new FileDownloadResult(DownloadStatus.NOT_FOUND, null, null, 0);
        }

        public static FileDownloadResult failure() {
            return new FileDownloadResult(DownloadStatus.ERROR, null, null, 0);
        }
    }

    public enum DownloadStatus {
        SUCCESS,
        UNAUTHORIZED,
        NOT_FOUND,
        ERROR
    }

    public ResponseBytes<GetObjectResponse> downloadFile(String key) {
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .key(key)
                            .bucket(bucket)
                            .build());
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Exception occurred while downloading key: {}", key, e);
            return null;
        }
    }

    public String deleteFile(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .key(key)
                            .bucket(bucket)
                            .build());
            return "Success";
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Exception occurred while deleting key: {}", key, e);
            return "Failure";
        }
    }

}
