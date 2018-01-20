package com.boe.domains;

import java.io.Serializable;

public class Document implements Serializable {
    private String fileName;
    private String sourcePath;
    private byte[] contents;
    private long convertTime;
    public Document(){};
    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getContents() {
        return contents;
    }

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public long getConvertTime() {
        return convertTime;
    }

    public void setConvertTime(long convertTime) {
        this.convertTime = convertTime;
    }
}
