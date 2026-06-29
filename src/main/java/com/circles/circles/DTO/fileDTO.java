package com.circles.circles.DTO;

import java.time.LocalDateTime;

public class fileDTO {
    private Long id;
    private String uploadedBy;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadedAt;
    private Long fileSize;
    
    public fileDTO() {
    }

    public fileDTO(Long id, String uploadedBy, String fileName, String fileType, LocalDateTime uploadedAt, Long fileSize) {
        this.id = id;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadedAt = uploadedAt;
        this.fileSize = fileSize;
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

    
    
}