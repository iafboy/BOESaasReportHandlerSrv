package com.boe.domains;

import org.springframework.http.HttpEntity;

import java.io.Serializable;
import java.sql.Timestamp;

public class ProxyRestRequest implements Serializable {

    private String requestID;
    private String TargetURL;
    private HttpEntity<Object> hpEntity;
    private Timestamp req_timestamp;
    private String respString;
    private Timestamp resp_timestamp;

    public ProxyRestRequest() {
    }

    public String getTargetURL() {
        return TargetURL;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public void setTargetURL(String targetURL) {
        TargetURL = targetURL;
    }

    public HttpEntity<Object> getHpEntity() {
        return hpEntity;
    }

    public void setHpEntity(HttpEntity<Object> hpEntity) {
        this.hpEntity = hpEntity;
    }

    public Timestamp getReq_timestamp() {
        return req_timestamp;
    }

    public void setReq_timestamp(Timestamp req_timestamp) {
        this.req_timestamp = req_timestamp;
    }

    public String getRespString() {
        return respString;
    }

    public void setRespString(String respString) {
        this.respString = respString;
    }

    public Timestamp getResp_timestamp() {
        return resp_timestamp;
    }

    public void setResp_timestamp(Timestamp resp_timestamp) {
        this.resp_timestamp = resp_timestamp;
    }
}
