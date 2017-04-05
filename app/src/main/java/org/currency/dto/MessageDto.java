package org.currency.dto;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.currency.App;
import org.currency.crypto.Encryptor;
import org.currency.crypto.PEMUtils;
import org.currency.dto.currency.CurrencyDto;
import org.currency.model.Currency;
import org.currency.socket.SocketOperation;
import org.currency.socket.Step;
import org.currency.util.JSON;
import org.currency.util.OperationType;
import org.currency.util.WebSocketSession;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(MessageDto.class.getName());

    private SocketOperation socketOperation;

    private OperationTypeDto operation;
    private Step step;
    private Integer statusCode;
    private String operationCode;
    private String deviceFromUUID;
    private String deviceToUUID;
    private String userFromName;
    private String userToName;
    private String deviceFromName;
    private String deviceToName;
    private String message;
    private String encryptedMessage;
    private String signedMessageBase64;
    private String subject;
    private String publicKeyPEM;
    private AESParamsDto aesParams;
    private String certificatePEM;
    private String base64Data;
    private boolean timeLimited = false;
    @JsonProperty("CurrencySet")
    private Set<CurrencyDto> currencyDtoSet;
    private Date date;
    private DeviceDto device;
    private String UUID;
    private String locale = Locale.getDefault().getDisplayLanguage();

    private UserDto user;
    private Set<Currency> currencySet;


    public MessageDto() {}

    public MessageDto getServerResponse(Integer statusCode, String message){
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setStatusCode(statusCode);
        socketMessageDto.setSocketOperation(SocketOperation.MSG_FROM_SERVER);
        socketMessageDto.setOperation(this.operation);
        socketMessageDto.setMessage(message);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public MessageDto getResponse(Integer statusCode, String message, String deviceFromUUID,
                  byte[] signedMessage, OperationTypeDto operation) throws Exception {
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToUUID(this.deviceFromUUID);
        MessageDto encryptedDto = new MessageDto();
        encryptedDto.setStatusCode(statusCode);
        encryptedDto.setMessage(message);
        if(signedMessage != null)
            encryptedDto.setSignedMessageBase64(Base64.encodeToString(signedMessage, Base64.NO_WRAP));
        encryptedDto.setDeviceFromUUID(deviceFromUUID);
        encryptedDto.setOperation(operation);
        encryptMessage(encryptedDto);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public Set<CurrencyDto> getCurrencyDtoSet() {
        return currencyDtoSet;
    }

    public void setCurrencyDtoSet(Set<CurrencyDto> currencyDtoSet) {
        this.currencyDtoSet = currencyDtoSet;
    }

    @JsonIgnore
    public boolean isEncrypted() {
        return encryptedMessage != null;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    public Step getStep() {
        return step;
    }

    public MessageDto setStep(Step step) {
        this.step = step;
        return this;
    }

    public SocketOperation getSocketOperation() {
        return socketOperation;
    }

    public MessageDto setSocketOperation(SocketOperation socketOperation) {
        this.socketOperation = socketOperation;
        return this;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public MessageDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getDeviceFromUUID() {
        return deviceFromUUID;
    }

    public MessageDto setDeviceFromUUID(String deviceFromUUID) {
        this.deviceFromUUID = deviceFromUUID;
        return this;
    }

    public String getDeviceToUUID() {
        return deviceToUUID;
    }

    public MessageDto setDeviceToUUID(String deviceToUUID) {
        this.deviceToUUID = deviceToUUID;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public MessageDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public Set<Currency> getCurrencySet() throws Exception {
        if(currencySet == null && currencyDtoSet != null) currencySet = CurrencyDto.deSerialize(currencyDtoSet);
        return currencySet;
    }

    public void setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public void setCertificatePEM(String certificatePEM) {
        this.certificatePEM = certificatePEM;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(String encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }

    public String getDeviceToName() {
        return deviceToName;
    }

    public void setDeviceToName(String deviceToName) {
        this.deviceToName = deviceToName;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public MessageDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getSignedMessageBase64() {
        return signedMessageBase64;
    }

    public MessageDto setSignedMessageBase64(String signedMessageBase64) {
        this.signedMessageBase64 = signedMessageBase64;
        return this;
    }

    public static MessageDto getCurrencyWalletChangeRequest(DeviceDto deviceFrom, DeviceDto deviceTo,
            List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, currencyList,
                OperationType.CURRENCY_WALLET_CHANGE);
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToUUID(deviceTo.getUUID());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        MessageDto encryptedDto = new MessageDto(deviceFrom.getDeviceName(), deviceFrom.getUUID(),
                deviceTo.getName(), deviceTo.getUUID(), null).setUserToName(
                deviceTo.getUserFullName()).setUserFromName(deviceFrom.getUserFullName());
        encryptedDto.setCurrencyDtoSet(CurrencyDto.serializeCollection(currencyList));
        encryptMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    public static MessageDto getMessageToDevice(String deviceFromName, String deviceFromUUID,
            DeviceDto deviceTo, String message) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, null, OperationType.MSG_TO_DEVICE);
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToUUID(deviceTo.getUUID());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        MessageDto encryptedDto = new MessageDto(deviceFromName, deviceFromUUID, deviceTo.getName(), deviceTo.getUUID(),
                message).setUserToName(deviceTo.getUserFullName()).setUserFromName(deviceFromUUID);
        encryptMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    public String getUserFromName() {
        return userFromName;
    }

    public MessageDto setUserFromName(String userFromName) {
        this.userFromName = userFromName;
        return this;
    }

    public String getUserToName() {
        return userToName;
    }

    public MessageDto setUserToName(String userToName) {
        this.userToName = userToName;
        return this;
    }

    public MessageDto(String deviceFromName, String deviceFromUUID, String deviceToName,
                      String deviceToUUID, String message) {
        this.deviceFromName = deviceFromName;
        this.deviceFromUUID = deviceFromUUID;
        this.deviceToName = deviceToName;
        this.deviceFromUUID = deviceToUUID;
        this.message = message;
    }

    public MessageDto getMessageResponse(String deviceFromName, String deviceFromUUID,
            String deviceToName, String message) throws Exception {
        WebSocketSession cd;
        MessageDto messageDto = new MessageDto();
        messageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        messageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        WebSocketSession socketSession = App.getInstance().getSocketSession(UUID);
        messageDto.setDeviceToUUID(deviceFromUUID);
        messageDto.setDeviceToName(deviceFromName);
        messageDto.setUUID(socketSession.getUUID());
        MessageDto encryptedDto = new MessageDto(deviceFromName, deviceFromUUID, deviceToName, this.getUUID(), message);
        encryptMessage(encryptedDto);
        return messageDto;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public MessageDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }

    private static void encryptMessage(MessageDto socketMessageDto,
                                       MessageDto encryptedDto, DeviceDto device) throws Exception {
        if(device.getX509Certificate() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), device.getX509Certificate());
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else if(device.getPublicKeyPEM() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(JSON.getMapper().writeValueAsBytes(encryptedDto),
                    PEMUtils.fromPEMToRSAPublicKey(device.getPublicKeyPEM().getBytes()));
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else log.log(Level.SEVERE, "Missing target public key info");
    }

    @JsonIgnore
    private void encryptMessage(MessageDto encryptedDto) throws Exception {
        if(certificatePEM != null) {
            X509Certificate targetDeviceCert = PEMUtils.fromPEMToX509Cert(certificatePEM.getBytes());
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), targetDeviceCert);
            encryptedMessage = new String(encryptedMessageBytes);
        } else if(publicKeyPEM != null) {
            PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(publicKeyPEM.getBytes());
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), publicKey);
            encryptedMessage = new String(encryptedMessageBytes);
        } else log.log(Level.SEVERE, "Missing target public key info");
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(encryptedMessage.getBytes(), privateKey);
        MessageDto decryptedDto = JSON.getMapper().readValue(decryptedBytes, MessageDto.class);
        this.operation = decryptedDto.getOperation();
        if(decryptedDto.getOperationCode() != null)
            operationCode = decryptedDto.getOperationCode();
        if(decryptedDto.getStatusCode() != null)
            statusCode = decryptedDto.getStatusCode();
        if(decryptedDto.getStep() != null)
            step = decryptedDto.getStep();
        if(decryptedDto.getDeviceFromName() != null)
            deviceFromName = decryptedDto.getDeviceFromName();
        if(decryptedDto.getDeviceFromUUID() != null)
            deviceFromUUID = decryptedDto.getDeviceFromUUID();
        if(decryptedDto.getSignedMessageBase64() != null)
            signedMessageBase64 = decryptedDto.getSignedMessageBase64();
        if(decryptedDto.getCurrencySet() != null)
            currencySet = CurrencyDto.deSerialize(decryptedDto.getCurrencyDtoSet());
        if(decryptedDto.getSubject() != null)
            subject = decryptedDto.getSubject();
        if(decryptedDto.getMessage() != null)
            message = decryptedDto.getMessage();
        if(decryptedDto.getDeviceToName() != null)
            deviceToName = decryptedDto.getDeviceToName();
        if(decryptedDto.getLocale() != null)
            locale = decryptedDto.getLocale();
        if(decryptedDto.getAesParams() != null)
            aesParams = decryptedDto.getAesParams();
        if(decryptedDto.getCertificatePEM() != null)
            certificatePEM = decryptedDto.getCertificatePEM();
        if(decryptedDto.getPublicKeyPEM() != null)
            publicKeyPEM = decryptedDto.getPublicKeyPEM();
        if(decryptedDto.getUUID() != null)
            UUID = decryptedDto.getUUID();
        timeLimited = decryptedDto.isTimeLimited();
        this.encryptedMessage = null;
    }

    private static <T> WebSocketSession checkWebSocketSession (DeviceDto device, T data,
            OperationType operationType) throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = null;
        if(device != null)
            webSocketSession = App.getInstance().getSocketSessionByDevice(device.getUUID());
        if(webSocketSession == null && device != null) {
            webSocketSession = new WebSocketSession(device).setUUID(
                    java.util.UUID.randomUUID().toString());
        }
        webSocketSession.setData(data);
        webSocketSession.setOperationType(operationType);
        App.getInstance().putSocketSession(webSocketSession.getUUID(), webSocketSession);
        return webSocketSession;
    }

}