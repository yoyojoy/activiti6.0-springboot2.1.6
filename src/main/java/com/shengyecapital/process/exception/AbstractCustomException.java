package com.shengyecapital.process.exception;

public abstract class AbstractCustomException extends RuntimeException {

    public AbstractCustomException(String message, Throwable t) {
        super(message, t);
    }

    public AbstractCustomException(String message) {
        super(message);
    }

    /**
     * code
     * @return
     */
    public abstract String getErrorCode();

}
