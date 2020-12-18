package com.cupshe.restclient.exception;

import com.cupshe.ak.text.StringUtils;

/**
 * ClassConvertException
 *
 * @author zxy
 */
public class ClassConvertException extends RuntimeException {

    private static final String MESSAGE = "Cannot serialize this return value: {}";

    public ClassConvertException(String stack, Exception e) {
        super(StringUtils.getFormatString(MESSAGE, stack), e);
    }
}
