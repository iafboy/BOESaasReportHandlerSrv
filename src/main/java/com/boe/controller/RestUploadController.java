package com.boe.controller;

import com.alibaba.fastjson.JSON;
import com.boe.domains.Document;
import com.boe.domains.UploadMetaData;
import com.boe.service.SevenZipService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class RestUploadController {
    private Logger logger = LoggerFactory.getLogger(RestUploadController.class);
    //Save the uploaded file to this local folder
    @Value("${uploaded.folder}")
    private String UPLOADED_FOLDER;

    @Value("${uploaded.succ.folder}")
    private String webSuccFolder;

    @Value("${uploaded.error.folder}")
    private String webErrorFolder;

    @Resource(name = "SevenZipService")
    private SevenZipService sevenZipService;

    @Autowired
    private AsyncRestTemplate srvRestTemplate;

    @Bean
    public AsyncRestTemplate srvRestTemplate() {
        return new AsyncRestTemplate();
    }

    @RequestMapping(value = "/restFileUpload", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    public UploadMetaData restFileUpload(@RequestParam("file") MultipartFile file) {
        long timeMills = System.currentTimeMillis();
        UploadMetaData umd = new UploadMetaData();
        if (file.isEmpty()) {
            umd.setErrorMsg("file is empty");
            umd.setFileName(file.getOriginalFilename());
            umd.setUploaded(false);
            umd.setHandleDate(timeMills);
        }
        try {
            logger.info("will save to " + UPLOADED_FOLDER + File.separator + file.getOriginalFilename());
            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            Path path = Paths.get(UPLOADED_FOLDER + File.separator + file.getOriginalFilename());
            Files.write(path, bytes);
            umd.setErrorMsg("");
            umd.setFileName(file.getOriginalFilename());
            umd.setUploaded(true);
            umd.setHandleDate(timeMills);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            umd.setErrorMsg(e.getMessage());
            umd.setFileName(file.getOriginalFilename());
            umd.setUploaded(false);
            umd.setHandleDate(timeMills);
        }
        return umd;
    }

    @RequestMapping(value = "/restUploadData", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    public UploadMetaData restSingleFileUpload(@Valid @RequestBody Document document) {
        if (document == null || document.getContents() == null) return null;
        long timeMills = System.currentTimeMillis();
        UploadMetaData umd = new UploadMetaData();
        if (document.getContents().length < 1) {
            umd.setErrorMsg("file is empty");
            umd.setFileName(document.getFileName());
            umd.setUploaded(false);
            umd.setHandleDate(timeMills);
        }
        try {
            // Get the file and save it somewhere
            byte[] bytes = document.getContents();
            Path path = Paths.get(UPLOADED_FOLDER + File.separator + document.getFileName());
            Files.write(path, bytes);
            umd.setErrorMsg("");
            umd.setFileName(document.getFileName());
            umd.setUploaded(true);
            umd.setHandleDate(timeMills);
            logger.info("saved to " + UPLOADED_FOLDER + File.separator + document.getFileName());
            byte[] contents = sevenZipService.deCompressDocumentToBytes(path.toFile());
            //restCall;
            //this.restPostCall(contents);
            FileUtils.copyFile(new File(UPLOADED_FOLDER + File.separator + document.getFileName()), new File(webSuccFolder + File.separator + System.currentTimeMillis() + "." + document.getFileName()));
            FileUtils.forceDelete(new File(UPLOADED_FOLDER + File.separator + document.getFileName()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            umd.setErrorMsg(e.getMessage());
            umd.setFileName(document.getFileName());
            umd.setUploaded(false);
            umd.setHandleDate(timeMills);
        }
        return umd;
    }

    private void restPostCall(byte[] document) {
        if (document == null || document.length < 1) return;
        long start = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<Object> hpEntity = new HttpEntity<Object>(JSON.toJSONString(document), headers);
        try {
            ListenableFuture<ResponseEntity<String>> future = srvRestTemplate.postForEntity("xxxxxxxxxxx", hpEntity, String.class);
            future.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
                public void onSuccess(ResponseEntity<String> resp) {
                    String restResp = resp.getBody();
                }

                public void onFailure(Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            });
            logger.debug("Thread " + Thread.currentThread().getName() + " rest call cost:" + (System.currentTimeMillis() - start));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);

        }
    }
}