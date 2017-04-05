package org.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;

import org.currency.util.JSON;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationDto implements Serializable {

    public static final String TAG = OperationDto.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    private OperationTypeDto operation;
    private Integer statusCode;
    private String subject;
    private String email;
    private String message;
    private String deviceId;
    private String httpSessionId;
    private String base64Data;
    private String callerCallback;
    private String userUUID;
    private String UUID;

    public OperationDto() { }

    public OperationDto(OperationTypeDto operationType) {
        this.operation = operationType;
    }

    public OperationDto(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public OperationDto(int statusCode, String message, OperationTypeDto operation) {
        this.statusCode = statusCode;
        this.message = message;
        this.operation = operation;
    }

    public String getUUID() {
        return UUID;
    }

    public OperationDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public void setOperation(OperationTypeDto operation) {
        this.operation = operation;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public void setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public OperationDto setUserUUID(String userUUID) {
        this.userUUID = userUUID;
        return this;
    }

    public <T> T getSignedContent(Class<T> type) throws Exception {
        if(message == null)
            return null;
        return JSON.readValue(JSON.writeValueAsString(message), type);
    }

    public <T> T getSignedContent(TypeReference<T> type) throws Exception {
        if(message == null)
            return null;
        return JSON.readValue(JSON.writeValueAsString(message), type);
    }

    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}