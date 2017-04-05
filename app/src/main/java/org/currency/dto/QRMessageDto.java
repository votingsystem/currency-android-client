package org.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.currency.crypto.PEMUtils;
import org.currency.util.Constants;
import org.currency.util.HashUtils;
import org.currency.util.OperationType;
import org.currency.util.QRUtils;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QRMessageDto<T> implements Serializable {

    public static final String TAG = QRMessageDto.class.getSimpleName();

    private static final long serialVersionUID = 1L;


    public static final String DEVICE_ID_KEY      = "did";
    public static final String ITEM_ID_KEY        = "iid";
    public static final String OPERATION_KEY      = "op";
    public static final String PUBLIC_KEY_KEY     = "pk";
    public static final String MSG_KEY            = "msg";
    public static final String SYSTEM_ENTITY_KEY  = "eid";
    public static final String URL_KEY            = "url";
    public static final String UUID_KEY           = "uid";

    private T data;
    private String originRevocationHash;
    private DeviceDto device;
    private AESParamsDto aesParams;


    private String deviceUUID;
    private String operation;
    private String operationCode;
    private String itemId;
    private Date dateCreated;
    private String revocationHash;
    private String publicKeyBase64;
    private OperationType operationType;
    private String msg;
    private String url;
    private String systemEntityId;
    private String UUID;

    public QRMessageDto() { }

    public QRMessageDto(DeviceDto deviceDto, org.currency.util.OperationType operationType) {
        this.operationType = operationType;
        this.deviceUUID = deviceDto.getUUID();
        this.dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0, 3);
    }

    public static QRMessageDto FROM_QR_CODE(String msg) {
        QRMessageDto qrMessageDto = new QRMessageDto();
        if (msg.contains(QRUtils.DEVICE_ID_KEY + "="))
            qrMessageDto.setDeviceUUID(msg.split(QRUtils.DEVICE_ID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.ITEM_ID_KEY + "="))
            qrMessageDto.setItemId(msg.split(QRUtils.ITEM_ID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.OPERATION_KEY + "="))
            qrMessageDto.setOperation(msg.split(QRUtils.OPERATION_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.SYSTEM_ENTITY_KEY + "="))
            qrMessageDto.setSystemEntityId(msg.split(QRUtils.SYSTEM_ENTITY_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.OPERATION_CODE_KEY + "="))
            qrMessageDto.setOperationCode(msg.split(QRUtils.OPERATION_CODE_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.PUBLIC_KEY_KEY + "="))
            qrMessageDto.setPublicKeyBase64(msg.split(QRUtils.PUBLIC_KEY_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.MSG_KEY + "="))
            qrMessageDto.setMsg(msg.split(QRUtils.MSG_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.UUID_KEY + "="))
            qrMessageDto.setUUID(msg.split(QRUtils.UUID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QRUtils.URL_KEY + "="))
            qrMessageDto.setUrl(msg.split(QRUtils.URL_KEY + "=")[1].split(";")[0]);
        return qrMessageDto;
    }

    public DeviceDto getDevice() throws Exception {
        if (device != null)
            return device;
        DeviceDto dto = new DeviceDto().setUUID(deviceUUID);
        if (publicKeyBase64 != null)
            dto.setPublicKey(PEMUtils.fromPEMToRSAPublicKey(publicKeyBase64.getBytes()));
        return dto;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public static String toQRCode(org.currency.util.OperationType operation, String deviceId) {
        StringBuilder result = new StringBuilder();
        if (deviceId != null)
            result.append(DEVICE_ID_KEY + "=" + deviceId + ";");
        if (operation != null)
            result.append(OPERATION_KEY + "=" + operation + ";");
        return result.toString();
    }

    public QRMessageDto createRequest() throws NoSuchAlgorithmException {
        this.originRevocationHash = java.util.UUID.randomUUID().toString();
        this.revocationHash = HashUtils.getHashBase64(originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public QRMessageDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public org.currency.util.OperationType getOperationType() {
        return operationType;
    }

    public QRMessageDto setOperationType(org.currency.util.OperationType operation) {
        this.operationType = operation;
        return this;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String key) {
        this.publicKeyBase64 = key;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSystemEntityId() {
        return systemEntityId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public QRMessageDto setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
        return this;
    }

    public QRMessageDto setSystemEntityId(String systemEntityId) {
        this.systemEntityId = systemEntityId;
        return this;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public QRMessageDto setOperationCode(String operationCode) {
        this.operationCode = operationCode;
        return this;
    }

    public QRMessageDto setOperation(String operation) {
        this.operation = operation;
        return this;
    }

    public String getOperation() {
        return this.operation;
    }

}