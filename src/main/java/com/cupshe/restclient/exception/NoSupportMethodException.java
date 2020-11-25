package com.cupshe.restclient.exception;

/**
 * NoSupportMethodException
 *
 * @author zxy
 */
public class NoSupportMethodException extends RuntimeException {

    private static final String MESSAGE = "Only supported method in [GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE].";

    public NoSupportMethodException() {
        super(MESSAGE);
    }
}
