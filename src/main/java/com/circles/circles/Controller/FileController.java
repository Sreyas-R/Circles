package com.circles.circles.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.circles.circles.Bizprocessor.FileProcessor;
import com.circles.circles.Bizprocessor.FileProcessor.FileDownloadResult;
import com.circles.circles.Model.ResponseObj;

import com.circles.circles.DTO.fileDTO;



@RestController
@RequestMapping("/file")
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final FileProcessor fileProcessor;

    public FileController(FileProcessor fileProcessor){
        this.fileProcessor = fileProcessor;
    }
    
    @PostMapping("/{circleId}/upload")
    public ResponseObj uploadFile(@RequestParam("file") MultipartFile file, @PathVariable("circleId") String circleId){
        Set<String> allowed = Set.of(
                "image/jpeg",
                "image/png",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        long maxSize = 10 * 1024 * 1024;
        ResponseObj res = new ResponseObj();

        if(file.isEmpty()){
            res.setErrorMessage("Empty_File");
            return res;
        }

        if(!allowed.contains(file.getContentType())){
            res.setErrorMessage("Invalid_File");
            return res;
        }
        if(file.getSize() > maxSize){
            res.setErrorMessage("File_Too_Large");
            return res;
        }
    
        logger.info("Received request to upload file for circle id: {}", circleId);

        try {
            String fileName = file.getOriginalFilename();
            InputStream inp = file.getInputStream();
            String fileType = file.getContentType();
            long size = file.getSize();
            
            String status = fileProcessor.uploadFile(circleId, fileName, inp, size , fileType);
            
            if ("Success".equals(status)) {
                res.setSuccMessage("File uploaded successfully");
            } else if ("Unauthorized".equals(status)) {
                res.setErrorMessage("UNAUTHORIZED");
            } else {
                res.setErrorMessage("S3_UPLOAD_FAILED");
            }
        } catch (NumberFormatException e) {
            res.setErrorMessage("INVALID_REQUEST");
        } catch (IOException e) {
            logger.error("Exception occurred while reading file stream for circleId: {}", circleId, e);
            res.setErrorMessage("INTERNAL_SERVER_ERROR");
        }
        return res;
    }

    
    @PostMapping(value = {"/{circleId}/view/documents", "/{circleId}/view/documents/{page}/{size}"})
    public ResponseEntity<ResponseObj> getDocuments(
            @PathVariable String circleId,
            @PathVariable(required = false) Integer page,
            @PathVariable(required = false) Integer size) {
        ResponseObj response = new ResponseObj();
        
        try {
            int pageVal = (page != null) ? page : 0;
            int sizeVal = (size != null) ? size : 20;
            logger.info("Fetching documents for circleId={}, page={}, size={}", circleId, pageVal, sizeVal);
            List<fileDTO> documents = fileProcessor.getAllDocs(circleId , pageVal , sizeVal);
            response.setSuccMessage("SUCCESS");
            response.setData(documents);
            logger.info("Fetched {} documents for circleId={}", documents.size(), circleId);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            response.setErrorMessage("INVALID_REQUEST");
            return ResponseEntity.badRequest().body(response);
        } catch (SecurityException e) {
            response.setErrorMessage("UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            logger.error("Failed to fetch documents for circleId={}", circleId, e);
            response.setErrorMessage("INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{circleId}/{documentId}/download")
    public ResponseEntity<ResponseObj> downloadDocument(@PathVariable String circleId , @PathVariable String documentId){
        ResponseObj response = new ResponseObj();

        try {
            logger.info("Creating download URL for circleId={}, documentId={}", circleId, documentId);
            FileDownloadResult downloadResult = fileProcessor.createDownloadUrl(circleId, documentId);

            switch (downloadResult.status()) {
                case SUCCESS:
                    response.setSuccMessage("SUCCESS");
                    response.setData(Map.of(
                            "downloadUrl", downloadResult.downloadUrl(),
                            "fileName", downloadResult.fileName(),
                            "expiresInSeconds", downloadResult.expiresInSeconds()));
                    return ResponseEntity.ok(response);
                case UNAUTHORIZED:
                    response.setErrorMessage("UNAUTHORIZED");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                case NOT_FOUND:
                    response.setErrorMessage("FILE_NOT_FOUND");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                default:
                    response.setErrorMessage("DOWNLOAD_URL_FAILED");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (NumberFormatException e) {
            response.setErrorMessage("INVALID_REQUEST");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Failed to create download URL for circleId={}, documentId={}", circleId, documentId, e);
            response.setErrorMessage("INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
}
