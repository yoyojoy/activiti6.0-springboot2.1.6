package com.shengyecapital.process.exception;

import org.springframework.http.HttpStatus;

/**
 * 服务端产生的异常
 */
public class ServerErrorException extends AbstractCustomException {

    public ServerErrorException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.toString();
    }

}
