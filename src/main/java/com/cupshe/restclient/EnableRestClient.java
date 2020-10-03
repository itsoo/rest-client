package com.cupshe.restclient;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * EnableRestClient
 *
 * @author zxy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RestClientRegister.class)
public @interface EnableRestClient {

    /**
     * 扫描包路径
     *
     * @return 可接收多个值数组
     */
    String[] basePackages() default {};
}
