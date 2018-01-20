package com.boe.client;

import com.alibaba.fastjson.JSON;
import com.boe.controller.UploadController;
import com.boe.domains.Document;
import com.boe.domains.UploadMetaData;
import com.boe.service.SevenZipService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.channels.FileChannel.MapMode;


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
    @Autowired
    private RestTemplate  resttemplate;
    @Bean
    public RestTemplate resttemplate(){return new RestTemplate();}
    @Autowired
    private AsyncRestTemplate  restTemplate;
    @Bean
    public AsyncRestTemplate  restTemplate () {
        return new AsyncRestTemplate  ();
    }
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
                boolean has = false;
                for (File wfile : workingfiles)
                    if (file.getName().equals(wfile.getName().substring(0, wfile.getName().length() - 3))) {
                        has = true;
                        break;
                    }
                if (!has) {
                    //RestTemplate restTemplate=new RestTemplate();
                    executor.submit(new UploadProceser(uploadServiceUrl, local7zPath, succFolder, errorFolder, sevenZipService, restTemplate,resttemplate,file));
                }
            }
        }
        //扫描工作文件夹剩余的文件继续上传
        for(File file:workingfiles){
            executor.submit(new UploadProceser(uploadServiceUrl, local7zPath, succFolder, errorFolder, sevenZipService,restTemplate, resttemplate,file));
        }
    }
}
class UploadProceser implements Runnable {
    private  Logger logger= LoggerFactory.getLogger(UploadProceser.class);
    private String uploadServiceUrl;
    private SevenZipService sevenZipService;
    private AsyncRestTemplate restTemplate;
    private RestTemplate resttemplate;
    private String local7zPath;
    private String succFolder;
    private String errorFolder;
    private File inFile;
    private String restResp;
    public UploadProceser(String uploadServiceUrl_,String local7zPath_,String succFolder_,String errorFolder_,SevenZipService sevenZipService_, AsyncRestTemplate restTemplate_,RestTemplate resttemplate,File file_){
        this.uploadServiceUrl=uploadServiceUrl_;
        this.local7zPath=local7zPath_;
        this.sevenZipService=sevenZipService_;
        this.restTemplate=restTemplate_;
        this.resttemplate=resttemplate;
        this.succFolder=succFolder_;
        this.errorFolder=errorFolder_;
        this.inFile=file_;
    }

    @Override
    public void run() {
        long beginTime=System.currentTimeMillis();
        //压缩文件
        File outfile=sevenZipService.compressDocument(inFile);
        //上传文件
        try {
            if (outfile != null) {
                Document doc=this.parseDocument(outfile);
                if(doc!=null)
                if (restPostDocument(doc)) {
                    //移动源文件至成功处理/失败文件夹
                    moveFile(outfile, true);
                    logger.debug(Thread.currentThread().getName()+" file uploaded and moved source file");
                } else {
                    moveFile(outfile, false);
                }
                //删除压缩文件
                if(outfile.exists())
                outfile.delete();
            } else {
                moveFile(inFile, false);
            }
            if (inFile.exists())
                inFile.delete();
        }catch (Exception ex){
            logger.error(ex.getMessage(),ex);
        }
        logger.debug(Thread.currentThread().getName()+" work done costs: "+(System.currentTimeMillis()-beginTime));
    }


    private Document parseDocument(File in){
        Document doc=null;
        if(in!=null) {
            doc=new Document();
            doc.setFileName(in.getName());
            doc.setConvertTime(System.currentTimeMillis());
            try {
                doc.setContents(toByteArray(in.getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return doc;
    }
    private byte[] toByteArray(String filename) throws Exception {
        FileChannel fc = null;
        try {
            fc = new RandomAccessFile(filename, "r").getChannel();
            MappedByteBuffer byteBuffer = fc.map(MapMode.READ_ONLY, 0,
                    fc.size()).load();
            System.out.println(byteBuffer.isLoaded());
            byte[] result = new byte[(int) fc.size()];
            if (byteBuffer.remaining() > 0) {
                // System.out.println("remain");
                byteBuffer.get(result, 0, byteBuffer.remaining());
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                fc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void moveFile(File inFile, boolean succ) {
        String folderPath;
        if(succ){
            folderPath=succFolder;
        }else{
            folderPath=errorFolder;
        }
        inFile.renameTo(new File(folderPath+File.separator+inFile.getName()+"."+String.valueOf(System.currentTimeMillis())+".7z"));
    }

    private boolean uploadDocument(Document document) {
        if(document==null||document.getContents()==null) return false;
        long start=System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", new FileSystemResource(new File(document.getSourcePath())));
        HttpEntity<Object> hpEntity = new HttpEntity<Object>(parts, headers);
        logger.debug("Thread "+Thread.currentThread().getName()+ "begin upload file");
        try {
            String restResp = resttemplate.postForObject(uploadServiceUrl, parts, String.class);
            logger.debug("Thread " + Thread.currentThread().getName() + " rest call cost:" +(System.currentTimeMillis()-start));
            UploadMetaData umd=null;
            if(restResp!=null)
                umd= JSON.parseObject(restResp, UploadMetaData.class);
            if (umd != null) {
                return true;
            }

        }catch(Exception ex){
            logger.error(ex.getMessage(),ex);
        }
        return false;
    }

    private boolean restPostDocument(Document document) {
        if(document==null||document.getContents()==null) return false;
        long start=System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<Object> hpEntity = new HttpEntity<Object>(JSON.toJSONString(document), headers);
        logger.debug("Thread "+Thread.currentThread().getName()+ " begin upload file "+document.getFileName());
        try {
            //HttpEntity<String> response = resttemplate.exchange(uploadServiceUrl, HttpMethod.POST, hpEntity, String.class);
            //if(response==null) return false;
            //restResp = response.getBody();
            ListenableFuture<ResponseEntity<String>> future = restTemplate.postForEntity(uploadServiceUrl, hpEntity, String.class);
            future.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
                public void onSuccess(ResponseEntity<String> resp) {
                    restResp = resp.getBody();
                }
                public void onFailure(Throwable t) {
                    logger.error(t.getMessage(),t);
                }
            });
            logger.debug("Thread " + Thread.currentThread().getName() + " rest call cost:" +(System.currentTimeMillis()-start));
            UploadMetaData umd=null;
            if(restResp!=null)
                umd= JSON.parseObject(restResp, UploadMetaData.class);
            if (umd != null) {
                return true;
            }

        }catch(Exception ex){
            logger.error(ex.getMessage(),ex);
        }
        return false;
    }
}
