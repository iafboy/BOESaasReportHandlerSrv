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

public class UploadFileTEST {
    private static File compressDocument(String file){
        if(file==null||"".equals(file.trim()))return null;
        File inFile = null;
        File outFile = null;
        BufferedInputStream inStream=null;
        BufferedOutputStream outStream=null;
        try {
            inFile = new File(file);
            outFile = new File(file+".7z");
            inStream = new BufferedInputStream(new FileInputStream(inFile));
            outStream = new BufferedOutputStream(new FileOutputStream(outFile));
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
            ex.printStackTrace();
        }finally {
            if(outStream!=null) {
                try {
                    outStream.flush();
                    outStream.close();
                } catch (Exception e) {
                   e.printStackTrace();
                }
            }
            if(inStream!=null)
                try {
                    inStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return null;
    }

    public static void main(String[] args){
        String file="D:\\test.xlsx";
        compressDocument(file);
    }

}
