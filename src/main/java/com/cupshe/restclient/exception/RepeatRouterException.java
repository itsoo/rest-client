package com.cupshe.restclient.exception;

import com.cupshe.ak.text.StringUtils;

/**
 * RepeatRouterException
 *
 * @author zxy
 */
public class RepeatRouterException extends RuntimeException {

    private static final String MESSAGE = "Repeated router of '{}', please checked 'rest-client.routers'" +
            " or set 'repeat-merged=true' of attribute 'rest-client'.";

    public RepeatRouterException(String routerName) {
        super(StringUtils.getFormatString(MESSAGE, routerName));
    }
}
