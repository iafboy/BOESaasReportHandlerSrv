package com.boe.service;

import SevenZip.Compression.LZMA.Decoder;
import com.boe.controller.UploadController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

@Service("SevenZipService")
public class SevenZipService implements ComprssServiceInf{
    private Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Value("${local.sevenzipfile.path}")
    private String local7zipPath;

    public File compressDocument(File file) {
        if (file == null ) return null;
        boolean result = false;
        File inFile = null;
        File outFile = null;
        BufferedInputStream inStream = null;
        BufferedOutputStream outStream = null;
        try {
            inFile = file;
            outFile = new File(local7zipPath + File.separator + inFile.getName() + ".7z");
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
            if (outFile.exists() && outFile.length() > 0) {
                result = true;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (outStream != null) {
                try {
                    outStream.flush();
                    outStream.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (inStream != null)
                try {
                    inStream.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
        }
        return outFile;
    }

    public byte[] compressDocumentBytes(byte[] inByte) {
        byte[] result=null;
        if(inByte==null) return null;
        ByteArrayInputStream inStream = null;
        ByteArrayOutputStream outStream = null;
        try {

            inStream = new ByteArrayInputStream(inByte);
            outStream = new ByteArrayOutputStream();
            boolean eos = false;
            SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
            encoder.SetEndMarkerMode(eos);
            encoder.WriteCoderProperties(outStream);
            encoder.Code(inStream, outStream, -1, -1, null);
            result=outStream.toByteArray();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (outStream != null) {
                try {
                    outStream.flush();
                    outStream.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (inStream != null)
                try {
                    inStream.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
        }
        return result;
    }

    @Value("${decompressfile.path}")
    private String decompressFilePath;

    public File deCompressDocument(File docPath) {
        if (docPath == null ) return null;
        boolean result = false;
        File fin = null;
        File fout = null;
        FileInputStream compIn = null;
        ByteArrayOutputStream decompOut = null;
        FileOutputStream fos = null;
        try {
            fin = docPath;
            fout = new File(decompressFilePath + File.separator + fin.getName().substring(0, fin.getName().length() - 3));
            fos = new FileOutputStream(fout);
            compIn = new FileInputStream(fin);
            decompOut = new ByteArrayOutputStream((int) fin.length());
            int propertiesSize = 5;
            byte[] properties = new byte[propertiesSize];
            if (compIn.read(properties, 0, propertiesSize) != propertiesSize) {
                throw new Exception("input .lzma file is too short");
            }

            Decoder decoder = new Decoder();
            if (!decoder.SetDecoderProperties(properties)) {
                throw new Exception("Incorrect stream properties");
            }
            long outSize = 0L;
            for (int i = 0; i < 8; ++i) {
                int v = compIn.read();
                if (v < 0) {
                    throw new Exception("Can't read stream size");
                }
                outSize |= (long) v << 8 * i;
            }
            decoder.Code(compIn, decompOut, outSize);
            decompOut.writeTo(fos);
            decompOut.flush();
            if(fout!=null&&fout.exists()&&fout.length()>0)
            return fout;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (decompOut != null) {
                try {
                    decompOut.flush();
                    decompOut.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (compIn != null)
                try {
                    compIn.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
        }
        return null;
    }
    public byte[] deCompressDocumentToBytes(File docPath) {
        byte[] result=null;
        if (docPath == null ) return null;
        File fin = null;
        FileInputStream compIn = null;
        ByteArrayOutputStream decompOut = null;
        FileOutputStream fos = null;
        try {
            fin = docPath;

            compIn = new FileInputStream(fin);
            decompOut = new ByteArrayOutputStream((int) fin.length());
            int propertiesSize = 5;
            byte[] properties = new byte[propertiesSize];
            if (compIn.read(properties, 0, propertiesSize) != propertiesSize) {
                throw new Exception("input .lzma file is too short");
            }

            Decoder decoder = new Decoder();
            if (!decoder.SetDecoderProperties(properties)) {
                throw new Exception("Incorrect stream properties");
            }
            long outSize = 0L;
            for (int i = 0; i < 8; ++i) {
                int v = compIn.read();
                if (v < 0) {
                    throw new Exception("Can't read stream size");
                }
                outSize |= (long) v << 8 * i;
            }
            decoder.Code(compIn, decompOut, outSize);
            result=decompOut.toByteArray();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (decompOut != null) {
                try {
                    decompOut.flush();
                    decompOut.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (compIn != null)
                try {
                    compIn.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
        }
        return result;
    }
}
