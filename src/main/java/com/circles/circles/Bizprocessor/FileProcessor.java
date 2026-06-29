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
        Long parsedCircleId = Long.parseLong(circleId);
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

    public List<fileDTO> getAllDocs(String circleId) {
        Long parsedCircleId = Long.parseLong(circleId);
        Long userId = getAuthenticatedUserId();

        if (!isCircleMember(userId, parsedCircleId)) {
            throw new SecurityException("User is not a member of this circle");
        }

        return fileRepository.getAllDocs(parsedCircleId);
    }

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
