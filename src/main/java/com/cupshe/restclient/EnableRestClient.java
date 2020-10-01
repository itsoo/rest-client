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

    /*** 扫描包 */
    String[] basePackages() default {};
}
