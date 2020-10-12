package com.cupshe.restclient.exception;

/**
 * ClassConvertException
 *
 * @author zxy
 */
public class ClassConvertException extends RuntimeException {

    private static final String MESSAGE = "Cannot serialize this return value: <<%s>>";

    public ClassConvertException(String stack, Exception e) {
        super(String.format(MESSAGE, stack), e);
    }
}
