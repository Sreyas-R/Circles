package com.circles.circles.DTO;

import java.time.LocalDateTime;

public class fileDTO {
    private Long id;
    private String uploadedBy;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadedAt;
    private Long fileSize;
    private String fileURL;
    private String fileS3Key; //THis shouldnt be sent to frontend , maybe unset it before sending
    
    
    public fileDTO() {
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public fileDTO(Long id, String uploadedBy, String fileName, String fileType, LocalDateTime uploadedAt, Long fileSize) {
        this.id = id;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadedAt = uploadedAt;
        this.fileSize = fileSize;
    }

    public fileDTO(Long id, String uploadedBy, String fileName, String fileType, LocalDateTime uploadedAt, Long fileSize, String fileS3Key) {
        this.id = id;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadedAt = uploadedAt;
        this.fileSize = fileSize;
        this.fileS3Key = fileS3Key;
    }

    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUploadedBy() {
        return uploadedBy;
    }
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getFileType() {
        return fileType;
    }
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    public Long getFileSize() {
        return fileSize;
    }
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileS3Key() {
        return fileS3Key;
    }

    public void setFileS3Key(String fileS3Key) {
        this.fileS3Key = fileS3Key;
    }

    
    
}