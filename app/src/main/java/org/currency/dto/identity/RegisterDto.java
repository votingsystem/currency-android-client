package org.currency.dto.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.currency.App;
import org.currency.dto.AddressDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.UserDto;
import org.currency.util.OperationType;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterDto {

    @JsonProperty("Operation")
    private OperationTypeDto operation;
    private AddressDto address;
    private UserDto user;
    private String csr;
    private String issuedCertificate;
    private String deviceId;
    private String UUID;

    public RegisterDto() {}

    public RegisterDto(UserDto user, String deviceId, String csr) {
        this.user = user;
        this.csr = csr;
        this.deviceId = deviceId;
        operation = new OperationTypeDto(OperationType.REGISTER_DEVICE,
                App.getInstance().getCurrencyService().getFirstIdentityProvider());
    }

    public static RegisterDto build(UserDto user, String deviceId, String mobileCsr) {
        RegisterDto dto = new RegisterDto(user, deviceId, mobileCsr);
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public String getCsr() {
        return csr;
    }

    public RegisterDto setCsr(String csr) {
        this.csr = csr;
        return this;
    }

    public UserDto getUser() {
        return user;
    }

    public RegisterDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public RegisterDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public RegisterDto setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getIssuedCertificate() {
        return issuedCertificate;
    }

    public void setIssuedCertificate(String issuedCertificate) {
        this.issuedCertificate = issuedCertificate;
    }
}