package org.currency.throwable;


import org.currency.dto.MessageDto;

public class BadRequestExceptionBase extends ExceptionBase {

    public BadRequestExceptionBase(String message) {
        super(message);
    }

    public BadRequestExceptionBase(String message, MessageDto messageDto) {
        super(message, messageDto);
    }
}
