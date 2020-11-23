package com.cupshe.restclient.exception;

/**
 * ConnectTimeoutException
 *
 * @author zxy
 */
public class ConnectTimeoutException extends RuntimeException {

    private static final String MESSAGE = "Connect timeout.";

    public ConnectTimeoutException() {
        super(MESSAGE);
    }
}
