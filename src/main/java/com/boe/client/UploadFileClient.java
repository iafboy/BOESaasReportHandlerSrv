package com.boe.client;

import ch.qos.logback.core.util.FileUtil;
import com.alibaba.fastjson.JSON;
import com.boe.controller.UploadController;
import com.boe.domains.Document;
import com.boe.domains.UploadMetaData;
import com.boe.service.SevenZipService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@RestController
public class UploadFileClient {
    private Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Value("${uploadservice.url}")
    private String uploadServiceUrl;

    @Value("${uploadwork.in.folder}")
    private String uploadWorkFolder;
    @Value("${uploadwork.succ.folder}")
    private String succFolder;
    @Value("${uploadwork.error.folder}")
    private String errorFolder;

    @Value("${local.sevenzipfile.path}")
    private String local7zPath;

    @Value("${clientworker.threadnum}")
    private int threadWorkerNum;

    @Value("${boe.schedule.trigger}")
    private boolean scheduleTrigger = false;

    @Resource(name = "SevenZipService")
    private SevenZipService sevenZipService;

    private ExecutorService executor;
    @Autowired
    private RestTemplate resttemplate;

    @Bean
    public RestTemplate resttemplate() {
        return new RestTemplate();
    }

    @Autowired
    private AsyncRestTemplate restTemplate;

    @Bean
    public AsyncRestTemplate restTemplate() {
        return new AsyncRestTemplate();
    }

    @PostConstruct
    public void createThreadPool() {
        executor = Executors.newFixedThreadPool(threadWorkerNum);
    }

    @Scheduled(cron = "${boe.schedule.cfg}")
    public void uploadFilesJob() {
        logger.debug(" Trigger check Schedule ");
        if (!scheduleTrigger) return;
        //扫描文件列表
        File uploadFolder = new File(uploadWorkFolder);
        File[] files = uploadFolder.listFiles();
        File[] workingfiles = new File(local7zPath).listFiles();
        //扫描工作文件夹剩余的文件继续上传
        for (File file : workingfiles) {
            executor.submit(new UploadProceser(uploadServiceUrl, local7zPath, succFolder, errorFolder, sevenZipService, restTemplate, resttemplate, file));
        }
        if (files != null && files.length > 0) {
            //分配多线程
            //  1.压缩文件
            //  2.批量上传
            for (File file : files) {
                boolean has = false;
                for (File wfile : workingfiles)
                    if (file.getName().equals(wfile.getName().substring(0, wfile.getName().length() - 3))) {
                        has = true;
                        break;
                    }
                if (!has) {
                    //RestTemplate restTemplate=new RestTemplate();
                    executor.submit(new UploadProceser(uploadServiceUrl, local7zPath, succFolder, errorFolder, sevenZipService, restTemplate, resttemplate, file));
                }
            }
        }
    }
}

class UploadProceser implements Runnable {
    private Logger logger = LoggerFactory.getLogger(UploadProceser.class);
    private String uploadServiceUrl;
    private SevenZipService sevenZipService;
    private AsyncRestTemplate restTemplate;
    private RestTemplate resttemplate;
    private String local7zPath;
    private String succFolder;
    private String errorFolder;
    private File inFile;
    private String restResp;

    public UploadProceser(String uploadServiceUrl_, String local7zPath_, String succFolder_, String errorFolder_, SevenZipService sevenZipService_, AsyncRestTemplate restTemplate_, RestTemplate resttemplate, File file_) {
        this.uploadServiceUrl = uploadServiceUrl_;
        this.local7zPath = local7zPath_;
        this.sevenZipService = sevenZipService_;
        this.restTemplate = restTemplate_;
        this.resttemplate = resttemplate;
        this.succFolder = succFolder_;
        this.errorFolder = errorFolder_;
        this.inFile = file_;
    }

    @Override
    public void run() {
        long beginTime = System.currentTimeMillis();
        //压缩文件
        File outfile = null;
        if (!".7z".equals(inFile.getName().substring(inFile.getName().length() - 3))) {
            outfile = sevenZipService.compressDocument(inFile);
            try {
                FileUtils.forceDelete(inFile);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            outfile = inFile;
        }
        if (outfile != null)
            //上传文件
            try {
                if (outfile != null) {
                    Document doc = this.parseDocument(outfile);
                    if (doc != null)
                        restPostDocument(doc);
                    //删除压缩文件
                    if (outfile.exists())
                        FileUtils.forceDelete(outfile);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        logger.debug(Thread.currentThread().getName() + " work done costs: " + (System.currentTimeMillis() - beginTime));
    }


    private Document parseDocument(File in) {
        Document doc = null;
        if (in != null) {
            doc = new Document();
            doc.setFileName(in.getName());
            doc.setConvertTime(System.currentTimeMillis());
            try {
                byte[] content = fileToByteArray(in.getAbsolutePath());
                doc.setContents(content);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return doc;
    }

    public byte[] fileToByteArray(String filename) throws Exception {
        byte[] result = null;
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }
        FileChannel channel = null;
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(f);
            channel = fs.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) channel.size());
            while ((channel.read(byteBuffer)) > 0) {
            }
            result = byteBuffer.array();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                channel.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            try {
                fs.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return result;
    }

    private void moveFile(File inFile, boolean succ) {
        String folderPath;
        if (succ) {
            folderPath = succFolder;
        } else {
            folderPath = errorFolder;
        }

        try {
            FileUtils.copyFile(inFile, new File(folderPath + File.separator + String.valueOf(System.currentTimeMillis()) + "." + inFile.getName()));
            FileUtils.forceDelete(inFile);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private boolean uploadDocumentFile(Document document) {
        if (document == null || document.getContents() == null) return false;
        long start = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", new FileSystemResource(new File(document.getSourcePath())));
        HttpEntity<Object> hpEntity = new HttpEntity<Object>(parts, headers);
        logger.debug("Thread " + Thread.currentThread().getName() + "begin upload file");
        try {
            String restResp = resttemplate.postForObject(uploadServiceUrl, parts, String.class);
            logger.debug("Thread " + Thread.currentThread().getName() + " rest call cost:" + (System.currentTimeMillis() - start));
            UploadMetaData umd = null;
            if (restResp != null)
                umd = JSON.parseObject(restResp, UploadMetaData.class);
            if (umd != null) {
                return true;
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return false;
    }

    private void restPostDocument(Document document) {
        if (document == null || document.getContents() == null) return;
        long start = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<Object> hpEntity = new HttpEntity<Object>(JSON.toJSONString(document), headers);
        logger.debug("Thread " + Thread.currentThread().getName() + " begin upload file " + document.getFileName());
        try {
            ListenableFuture<ResponseEntity<String>> future = restTemplate.postForEntity(uploadServiceUrl, hpEntity, String.class);
            future.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
                public void onSuccess(ResponseEntity<String> resp) {
                    restResp = resp.getBody();
                }

                public void onFailure(Throwable t) {
                    logger.error(t.getMessage(), t);
                    moveFile(new File(local7zPath + File.separator + document.getFileName()), false);
                }
            });
            logger.debug("Thread " + Thread.currentThread().getName() + " rest call cost:" + (System.currentTimeMillis() - start));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            moveFile(new File(local7zPath + File.separator + document.getFileName()), false);
        }
        moveFile(new File(local7zPath + File.separator + document.getFileName()), true);
    }
}
