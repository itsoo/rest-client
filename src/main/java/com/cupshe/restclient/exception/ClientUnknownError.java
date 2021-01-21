package com.cupshe.restclient.exception;

/**
 * UnknownError
 *
 * @author zxy
 */
public class ClientUnknownError extends RuntimeException {

    private static final String MESSAGE = "Unknown error, there is no timeout, no return value and no fallback.";

    public ClientUnknownError() {
        super(MESSAGE);
    }
}
