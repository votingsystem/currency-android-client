package org.currency.throwable;


import org.currency.dto.MessageDto;

public class ServerException extends ExceptionBase {

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, MessageDto messageDto) {
        super(message, messageDto);
    }
}
