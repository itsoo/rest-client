package com.cupshe.restclient.lang;

import com.cupshe.restclient.exception.ConnectTimeoutException;
import org.springframework.core.annotation.AliasFor;

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

    /**
     * Spring 容器中对象的名称（默认为目标类对应的属性名）
     *
     * @return bean name
     */
    String id() default "";

    /**
     * 服务名称
     * <ol>
     *   <li>name 与 value 不能同时为空</li>
     *   <li>与 value 同时设置时权重高于 value</li>
     * </ol>
     *
     * @return service name
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 服务名称
     * <ol>
     *   <li>name 与 value 不能同时为空</li>
     *   <li>与 name 同时设置时权重低于 name</li>
     * </ol>
     *
     * @return service name
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 请求 URI 上下文（即 path 的前缀部分）
     *
     * @return prefix path
     */
    String path() default "";

    /**
     * 负载均衡策略
     *
     * @return {@link RestClient.LoadBalanceType#RR}
     */
    LoadBalanceType loadBalanceType() default LoadBalanceType.RR;

    /**
     * 最大重试次数，第一次请求不计入重试次数（即存在失败情况下的总请求次数为 maxAutoRetries + 1）
     *
     * @return int
     */
    int maxAutoRetries() default 0;

    /**
     * 失败时的兜底类类型
     *
     * @return Class
     * @throws ConnectTimeoutException 若未设置 fallback 请求失败时将抛出异常
     */
    Class<?> fallback() default void.class;

    /**
     * 请求连接超时时间（ms）
     *
     * @return int
     */
    int connectTimeout() default 1000;

    /**
     * 等待响应超时时间（ms）
     *
     * @return int
     */
    int readTimeout() default -1;

    /**
     * 负载均衡策略枚举
     */
    enum LoadBalanceType {
        /*** 轮询（Round-Robin） */
        RR,

        /*** 随机（Random） */
        R
    }
}
