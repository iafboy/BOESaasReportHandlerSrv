package com.boe.service;

import java.io.File;

public interface ComprssServiceInf {
    public File compressDocument(File file);
    public byte[] compressDocumentBytes(byte[] inByte);
    public File deCompressDocument(File docPath);
    public byte[] deCompressDocumentToBytes(File docPath);
}
