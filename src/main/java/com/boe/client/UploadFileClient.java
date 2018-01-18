package com.boe.client;

import com.boe.controller.UploadController;
import com.boe.service.SevenZipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@RestController
public class UploadFileClient {
    private  Logger logger= LoggerFactory.getLogger(UploadController.class);

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
    private boolean scheduleTrigger=false;

    @Resource(name="SevenZipService")
    private SevenZipService sevenZipService;

    private ExecutorService executor;

    @PostConstruct
    public void createThreadPool(){
        executor = Executors.newFixedThreadPool(threadWorkerNum);
    }

    @Scheduled(cron = "${boe.schedule.cfg}")
    public void uploadFilesJob(){
        logger.debug(" Trigger check Schedule ");
        if(!scheduleTrigger) return;
        //扫描文件列表
        File uploadFolder=new File(uploadWorkFolder);
        File[] files=uploadFolder.listFiles();
        File[] workingfiles=new File(local7zPath).listFiles();
        if(files!=null&&files.length>0) {
            //分配多线程
            //  1.压缩文件
            //  2.批量上传
            for(File file:files) {
                boolean has=false;
                for(File wfile:workingfiles)
                    if(file.getName().equals(wfile.getName().substring(0,wfile.getName().length()-3))) {
                        has=true;
                        break;
                    }
                if(!has) {
                    RestTemplate restTemplate = new RestTemplate();
                    executor.submit(new UploadProceser(uploadServiceUrl, local7zPath, succFolder, errorFolder, sevenZipService,restTemplate, file));
                }
            }
        }
    }

}
class UploadProceser implements Runnable {
    private  Logger logger= LoggerFactory.getLogger(UploadProceser.class);
    private String uploadServiceUrl;
    private SevenZipService sevenZipService;
    private RestTemplate restTemplate;
    private String local7zPath;
    private String succFolder;
    private String errorFolder;
    private File inFile;

    public UploadProceser(String uploadServiceUrl_,String local7zPath_,String succFolder_,String errorFolder_,SevenZipService sevenZipService_, RestTemplate restTemplate_,File file_){
        this.uploadServiceUrl=uploadServiceUrl_;
        this.local7zPath=local7zPath_;
        this.sevenZipService=sevenZipService_;
        this.restTemplate=restTemplate_;
        this.succFolder=succFolder_;
        this.errorFolder=errorFolder_;
        this.inFile=file_;
    }

    @Override
    public void run() {
        logger.debug(Thread.currentThread().getName()+" begin handler file "+inFile.getAbsolutePath());
        //压缩文件
        File outfile=sevenZipService.compressDocument(inFile);
        //上传文件
        if(outfile!=null){
            uploadDocument(outfile);
            //删除压缩文件
            outfile.delete();
            //移动源文件至成功处理/失败文件夹
            moveFile(inFile,true);
        }else{
            moveFile(inFile,false);
        }
        if(inFile.exists())
            inFile.delete();
        logger.debug(Thread.currentThread().getName()+" work done");
    }

    private void moveFile(File inFile, boolean succ) {
        String folderPath;
        if(succ){
            folderPath=succFolder;
        }else{
            folderPath=errorFolder;
        }
        inFile.renameTo(new File(folderPath+File.separator+inFile.getName()+"-"+String.valueOf(System.currentTimeMillis())));
    }

    private void uploadDocument(File document) {
        if(document==null||document.length()<1) return;
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", new FileSystemResource(document.getAbsoluteFile()));
        logger.debug("Thread "+Thread.currentThread().getName()+ "begin upload file");
        String result = restTemplate.postForObject(uploadServiceUrl, parts,String.class);
        logger.debug("Thread "+Thread.currentThread().getName()+ " return upload result:\n"+result.toString());
    }
}
