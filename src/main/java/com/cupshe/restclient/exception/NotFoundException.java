package com.cupshe.restclient.exception;

/**
 * NotFoundException
 *
 * @author zxy
 */
public class NotFoundException extends RuntimeException {

    private static final String MESSAGE = "Not found.";

    public NotFoundException() {
        super(MESSAGE);
    }
}
