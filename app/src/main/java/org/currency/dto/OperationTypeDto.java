package org.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.currency.util.OperationType;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationTypeDto<T> {

    private OperationType type;
    private String entityId;

    public OperationTypeDto() {}

    public OperationTypeDto(OperationType type, String entityId) {
        this.type = type;
        this.entityId = entityId;
    }

    public OperationType getType() {
        return type;
    }

    public OperationTypeDto setType(OperationType type) {
        this.type = type;
        return this;
    }

    public String getEntityId() {
        return entityId;
    }

    public OperationTypeDto setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

}