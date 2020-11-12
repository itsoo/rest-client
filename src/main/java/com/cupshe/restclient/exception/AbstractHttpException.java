package com.cupshe.restclient.exception;

/**
 * AbstractHttpException
 *
 * @author zxy
 */
public abstract class AbstractHttpException extends RuntimeException {

    /**
     * 获取状态码
     *
     * @return int
     */
    public abstract int getStatusCode();

    /**
     * 获取消息
     *
     * @return String
     */
    @Override
    public abstract String getMessage();
}
