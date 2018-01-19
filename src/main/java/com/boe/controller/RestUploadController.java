package com.boe.controller;

import com.boe.domains.UploadMetaData;
import com.boe.service.SevenZipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class RestUploadController {
    private Logger logger= LoggerFactory.getLogger(RestUploadController.class);
    //Save the uploaded file to this local folder
    @Value("${uploaded.folder}")
    private String UPLOADED_FOLDER;

    @Value("${uploaded.succ.folder}")
    private String webSuccFolder;

    @Value("${uploaded.error.folder}")
    private String webErrorFolder;

    @Resource(name="SevenZipService")
    private SevenZipService sevenZipService;

    @RequestMapping(value = "/restSingleFileUpload", method = RequestMethod.POST,produces="application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    public UploadMetaData restFileUpload(@RequestParam("file") MultipartFile file){
        long timeMills=System.currentTimeMillis();
        UploadMetaData umd=new UploadMetaData();
        if (file.isEmpty()) {
            umd.setErrorMsg("file is empty");
            umd.setFileName(file.getOriginalFilename());
            umd.setUploaded(false);
            umd.setHandleDate(timeMills);
        }
        try {
            logger.info("will save to "+UPLOADED_FOLDER+ File.separator+file.getOriginalFilename());
            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            Path path = Paths.get(UPLOADED_FOLDER + File.separator+ file.getOriginalFilename());
            Files.write(path, bytes);
            umd.setErrorMsg("");
            umd.setFileName(file.getOriginalFilename());
            umd.setUploaded(true);
            umd.setHandleDate(timeMills);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            umd.setErrorMsg(e.getMessage());
            umd.setFileName(file.getOriginalFilename());
            umd.setUploaded(false);
            umd.setHandleDate(timeMills);
        }
        return umd;
    }
    private String deCompressFile(File document){

        return null;
    }
}