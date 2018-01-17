package com.boe.controller;

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

@RestController
public class UploadFileClient {
    private  Logger logger= LoggerFactory.getLogger(UploadController.class);
    @Value("${uploadservice.url}")
    private String uploadServiceUrl;
    private  File compressDocument(String file){
        if(file==null||"".equals(file.trim()))return null;
        File inFile = null;
        File outFile = null;
        BufferedInputStream inStream=null;
        BufferedOutputStream outStream=null;
        try {
            inFile = new File(file);
            outFile = new File(file+".7z");
            inStream = new BufferedInputStream(new java.io.FileInputStream(inFile));
            outStream = new BufferedOutputStream(new java.io.FileOutputStream(outFile));
            boolean eos = false;
            SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
            encoder.SetEndMarkerMode(eos);
            encoder.WriteCoderProperties(outStream);
            long fileSize;
            if (eos)
                fileSize = -1;
            else
                fileSize = inFile.length();
            for (int i = 0; i < 8; i++)
                outStream.write((int) (fileSize >>> (8 * i)) & 0xFF);
            encoder.Code(inStream, outStream, -1, -1, null);
            if(outFile.exists()&&outFile.length()>0){
                return outFile;
            }
        }catch(Exception ex){
            logger.error(ex.getMessage(),ex);
        }finally {
            if(outStream!=null) {
                try {
                    outStream.flush();
                    outStream.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if(inStream!=null)
                try {
                    inStream.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
        }
        return null;
    }
    private  String uploadDocument(File document) {
        if(document==null||document.length()<1) return null;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", new FileSystemResource(document.getAbsoluteFile()));
        String result = restTemplate.postForObject(uploadServiceUrl, parts,String.class);
        logger.debug("return result:\n"+result.toString());
        return null;
    }
}
