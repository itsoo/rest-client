package com.cupshe.restclient;

import java.lang.annotation.*;

/**
 * RestClient
 *
 * @author zxy
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestClient {

    /*** service name */
    String value();

    /*** prefix uri */
    String path() default "";

    /*** 负载均衡策略 */
    LoadBalanceType loadBalanceType() default LoadBalanceType.RR;

    /*** 最大重试次数 */
    int maxAutoRetries() default 0;

    /*** fallback eg.('@com.examples.Demo#abc') */
    String fallback() default "";

    /*** 连接超时时间 */
    long connectTimeout() default 1000L;

    /**
     * 负载均衡策略枚举
     */
    enum LoadBalanceType {
        /*** 轮询 Round-Robin */
        RR,

        /*** 随机 Random */
        R
    }
}
