package com.cupshe.restclient.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * NotFoundException
 *
 * @author zxy
 */
public class NotFoundException extends AbstractHttpException {

    @Override
    public int getStatusCode() {
        return HttpServletResponse.SC_NOT_FOUND;
    }

    @Override
    public String getMessage() {
        return "Not found";
    }
}
