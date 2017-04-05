package org.currency.http;

import org.currency.crypto.CertificationRequest;
import org.currency.crypto.PEMUtils;
import org.currency.dto.DeviceDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.UserDto;
import org.currency.dto.identity.SessionCertificationDto;
import org.currency.util.OperationType;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionInfo implements Serializable {

    private DeviceDto sessionDevice;
    private UserDto user;
    private CertificationRequest mobileCsrReq;
    private CertificationRequest browserCsrReq;
    private SessionCertificationDto sessionCertification;
    private String entityId;


    public SessionInfo() {}

    public SessionInfo(CertificationRequest mobileCsrReq, CertificationRequest browserCsrReq) {
        this.mobileCsrReq = mobileCsrReq;
        this.browserCsrReq = browserCsrReq;
    }

    public CertificationRequest getMobileCsrReq() {
        return mobileCsrReq;
    }

    public SessionInfo setMobileCsrReq(CertificationRequest mobileCsrReq) {
        this.mobileCsrReq = mobileCsrReq;
        return this;
    }

    public CertificationRequest getBrowserCsrReq() {
        return browserCsrReq;
    }

    public SessionInfo setBrowserCsrReq(CertificationRequest browserCsrReq) {
        this.browserCsrReq = browserCsrReq;
        return this;
    }

    public DeviceDto getSessionDevice() {
        return sessionDevice;
    }

    public SessionInfo setSessionDevice(DeviceDto sessionDevice) {
        this.sessionDevice = sessionDevice;
        return this;
    }

    public UserDto getUser() {
        return user;
    }

    public SessionInfo setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public void loadIssuedCerts(SessionCertificationDto certificationDto) {
        sessionCertification = certificationDto;
        mobileCsrReq.setSignedCsr(certificationDto.getMobileCsrSigned().getBytes());
        browserCsrReq.setSignedCsr(certificationDto.getBrowserCsrSigned().getBytes());
    }

    public String getEntityId() {
        return entityId;
    }

    public SessionInfo setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public SessionCertificationDto buildBrowserCertificationDto() throws Exception {
        SessionCertificationDto sessionCertificationDto = new SessionCertificationDto();
        sessionCertificationDto.setOperation(new OperationTypeDto(OperationType.SESSION_CERTIFICATION, entityId))
                .setPrivateKeyPEM(new String(PEMUtils.getPEMEncoded(browserCsrReq.getPrivateKey())))
                .setMobileUUID(sessionCertification.getMobileUUID())
                .setMobileCsrSigned(sessionCertification.getMobileCsrSigned())
                .setBrowserUUID(sessionCertification.getBrowserUUID())
                .setBrowserCsrSigned(sessionCertification.getBrowserCsrSigned())
                .setUser(sessionCertification.getUser());
        return sessionCertificationDto;
    }

}