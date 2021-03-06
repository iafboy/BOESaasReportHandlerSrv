package com.boe.domains;

import java.sql.Date;

public class UploadMetaData {
    private String fileName;
    private long handleDate;
    private boolean uploaded;
    private String errorMsg;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getHandleDate() {
        return handleDate;
    }

    public void setHandleDate(long handleDate) {
        this.handleDate = handleDate;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
