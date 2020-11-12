package com.cupshe.restclient.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * BadRequestException
 *
 * @author zxy
 */
public class BadRequestException extends AbstractHttpException {

    @Override
    public int getStatusCode() {
        return HttpServletResponse.SC_BAD_REQUEST;
    }

    @Override
    public String getMessage() {
        return "Bad request";
    }
}
