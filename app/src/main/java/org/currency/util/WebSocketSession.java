package org.currency.util;

import org.currency.dto.AESParamsDto;
import org.currency.dto.DeviceDto;
import org.currency.dto.MessageDto;
import org.currency.dto.QRMessageDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private OperationType operationType;
    private T data;
    private MessageDto lastMessage;
    private AESParamsDto aesParams;;
    private QRMessageDto qrMessage;
    private DeviceDto device;
    private String broadCastId;
    private String UUID;

    public WebSocketSession(MessageDto socketMsg) {
        this.operationType = socketMsg.getOperation().getType();
        this.lastMessage = socketMsg;
        this.UUID = socketMsg.getUUID();
    }

    public WebSocketSession(DeviceDto device) {
        this.device = device;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getUUID() {
        return UUID;
    }

    public WebSocketSession setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getBroadCastId() {
        return broadCastId;
    }

    public WebSocketSession setBroadCastId(String broadCastId) {
        this.broadCastId = broadCastId;
        return this;
    }

    public MessageDto getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageDto lastMessage) {
        this.lastMessage = lastMessage;
    }

    public QRMessageDto getQrMessage() {
        return qrMessage;
    }

    public void setQrMessage(QRMessageDto qrMessage) {
        this.qrMessage = qrMessage;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
    }

}
