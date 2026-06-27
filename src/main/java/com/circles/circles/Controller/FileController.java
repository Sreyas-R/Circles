package com.circles.circles.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.circles.circles.Bizprocessor.FileProcessor;
import com.circles.circles.Model.ResponseObj;

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
        Set<String> allowed = Set.of("image/jpeg", "image/png", "application/pdf", "application/msword");
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
            
            // Pass circleId, fileName, stream, and size to the processor
            String status = fileProcessor.uploadFile(circleId, fileName, inp, size , fileType);
            
            if ("Success".equals(status)) {
                res.setSuccMessage("File uploaded successfully");
            } else {
                res.setErrorMessage("S3_UPLOAD_FAILED");
            }
        } catch (IOException e) {
            logger.error("Exception occurred while reading file stream for circleId: {}", circleId, e);
            res.setErrorMessage("INTERNAL_SERVER_ERROR");
        }
        return res;
    }
}
