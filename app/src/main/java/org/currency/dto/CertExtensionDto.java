package org.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertExtensionDto implements Serializable {

    private static final long serialVersionUID = 1L;


    private String deviceName;
    private String email;
    private String mobilePhone;
    private String numId;
    private String givenname;
    private String surname;
    private DeviceDto.Type deviceType;
    private String UUID;

    public CertExtensionDto() {}

    public CertExtensionDto(String numId , String givenname, String surname) {
        this.numId = numId;
        this.givenname = givenname;
        this.surname = surname;
    }

    public CertExtensionDto(String deviceName, String UUID, String email, String phone,
                            DeviceDto.Type deviceType) {
        this.UUID = UUID;
        this.deviceName = deviceName;
        this.email = email;
        this.mobilePhone = phone;
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public DeviceDto.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceDto.Type deviceType) {
        this.deviceType = deviceType;
    }

    public String getGivenname() {
        return givenname;
    }

    public CertExtensionDto setGivenname(String givenname) {
        this.givenname = givenname;
        return this;
    }

    public String getSurname() {
        return surname;
    }

    public CertExtensionDto setSurname(String surname) {
        this.surname = surname;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public CertExtensionDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    @JsonIgnore
    public String getPrincipal() {
        return "SERIALNUMBER=" + numId + ", GIVENNAME=" + givenname + ", SURNAME=" + surname;
    }

    public String getNumId() {
        return numId;
    }

    public CertExtensionDto setNumId(String numId) {
        this.numId = numId;
        return this;
    }

}