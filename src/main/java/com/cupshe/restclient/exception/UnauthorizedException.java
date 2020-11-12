package com.cupshe.restclient.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * UnauthorizedException
 *
 * @author zxy
 */
public class UnauthorizedException extends AbstractHttpException {

    @Override
    public int getStatusCode() {
        return HttpServletResponse.SC_UNAUTHORIZED;
    }

    @Override
    public String getMessage() {
        return "Unauthorized";
    }
}
