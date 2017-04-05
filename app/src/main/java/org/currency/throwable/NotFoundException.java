package org.currency.throwable;


import org.currency.dto.MessageDto;

public class NotFoundException extends ExceptionBase {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, MessageDto messageDto) {
        super(message, messageDto);
    }

}
