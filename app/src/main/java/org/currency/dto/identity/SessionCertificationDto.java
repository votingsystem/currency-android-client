package org.currency.dto.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.currency.dto.AddressDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.UserDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionCertificationDto {

    private OperationTypeDto operation;

    private Integer statusCode;

    private AddressDto address;
    private UserDto user;
    private String signerCertPEM;

    private String browserCsr;
    private String browserCsrSigned;
    private String browserUUID;

    private String mobileCsr;
    private String mobileCsrSigned;
    private String mobileUUID;

    private String privateKeyPEM;
    private String publicKeyPEM;
    private String token;
    private String userUUID;


    public SessionCertificationDto() { }

    public SessionCertificationDto(UserDto user, String mobileCsr,  String mobileUUID,
                                   String browserCsr, String browserUUID) {
        this.user = user;
        this.mobileCsr = mobileCsr;
        this.mobileUUID = mobileUUID;
        this.browserCsr = browserCsr;
        this.browserUUID = browserUUID;
    }

    public SessionCertificationDto(OperationTypeDto operationType) {
        this.operation = operationType;
    }

    public SessionCertificationDto(AddressDto address, String browserCsr, String token) {
        this.address = address;
        this.browserCsr = browserCsr;
        this.token = token;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public SessionCertificationDto setUserUUID(String userUUID) {
        this.userUUID = userUUID;
        return this;
    }

    public String getMobileCsr() {
        return mobileCsr;
    }

    public SessionCertificationDto setMobileCsr(String mobileCsr) {
        this.mobileCsr = mobileCsr;
        return this;
    }

    public String getMobileCsrSigned() {
        return mobileCsrSigned;
    }

    public SessionCertificationDto setMobileCsrSigned(String mobileCsrSigned) {
        this.mobileCsrSigned = mobileCsrSigned;
        return this;
    }

    public String getBrowserCsr() {
        return browserCsr;
    }

    public SessionCertificationDto setBrowserCsr(String browserCsr) {
        this.browserCsr = browserCsr;
        return this;
    }

    public String getBrowserCsrSigned() {
        return browserCsrSigned;
    }

    public SessionCertificationDto setBrowserCsrSigned(String browserCsrSigned) {
        this.browserCsrSigned = browserCsrSigned;
        return this;
    }

    public UserDto getUser() {
        return user;
    }

    public SessionCertificationDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public SessionCertificationDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getSignerCertPEM() {
        return signerCertPEM;
    }

    public SessionCertificationDto setSignerCertPEM(String signerCertPEM) {
        this.signerCertPEM = signerCertPEM;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public SessionCertificationDto setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getBrowserUUID() {
        return browserUUID;
    }

    public SessionCertificationDto setBrowserUUID(String browserUUID) {
        this.browserUUID = browserUUID;
        return this;
    }

    public String getMobileUUID() {
        return mobileUUID;
    }

    public SessionCertificationDto setMobileUUID(String mobileUUID) {
        this.mobileUUID = mobileUUID;
        return this;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public SessionCertificationDto setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
        return this;
    }

    public String getPrivateKeyPEM() {
        return privateKeyPEM;
    }

    public SessionCertificationDto setPrivateKeyPEM(String privateKeyPEM) {
        this.privateKeyPEM = privateKeyPEM;
        return this;
    }

}