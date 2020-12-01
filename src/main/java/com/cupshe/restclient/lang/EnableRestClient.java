package com.cupshe.restclient.lang;

import com.cupshe.restclient.RestClientRegister;
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
     * @return Array of String
     */
    String[] basePackages() default {};
}