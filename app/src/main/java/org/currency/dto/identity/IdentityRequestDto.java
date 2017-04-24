package org.currency.dto.identity;

import org.currency.util.OperationType;

import java.io.Serializable;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class IdentityRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private OperationType type;
    private Date date;
    private String revocationHash;
    private String indentityServiceEntity;
    private String callbackServiceEntityId;
    private String UUID;

    public IdentityRequestDto() { }

    public IdentityRequestDto(OperationType type, String indentityServiceEntity,
                              String callbackServiceEntityId) {
        this.type = type;
        this.indentityServiceEntity = indentityServiceEntity;
        this.callbackServiceEntityId = callbackServiceEntityId;
    }

    public IdentityRequestDto(OperationType type) {
        this.type = type;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public String getUUID() {
        return UUID;
    }

    public IdentityRequestDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getCallbackServiceEntityId() {
        return callbackServiceEntityId;
    }

    public void setCallbackServiceEntityId(String callbackServiceEntityId) {
        this.callbackServiceEntityId = callbackServiceEntityId;
    }

    public String getIndentityServiceEntity() {
        return indentityServiceEntity;
    }

    public void setIndentityServiceEntity(String indentityServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
    }

    public Date getDate() {
        return date;
    }

    public IdentityRequestDto setDate(Date date) {
        this.date = date;
        return this;
    }
}