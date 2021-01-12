package com.cupshe.restclient.exception;

import com.cupshe.ak.text.StringUtils;

import javax.servlet.http.HttpServletResponse;

/**
 * NotFoundException
 *
 * @author zxy
 */
public class NotFoundException extends RuntimeException {

    private static final int STATUS_CODE = HttpServletResponse.SC_NOT_FOUND;

    private static final String MESSAGE = "{} Not found";

    public NotFoundException() {
        super(StringUtils.getFormatString(MESSAGE, STATUS_CODE));
    }
}
