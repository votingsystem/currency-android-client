package org.currency.throwable;


import org.currency.dto.MessageDto;

public class RequestRepeatedException extends ExceptionBase {

    public RequestRepeatedException(String message) {
        super(message);
    }

    public RequestRepeatedException(String message, MessageDto messageDto) {
        super(message, messageDto);
    }

}
