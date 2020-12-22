package com.cupshe.restclient.lang;

import java.lang.annotation.*;

/**
 * PureFunction
 * <p>Used to describe class or method as a pure function.
 *
 * @author zxy
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface PureFunction {
    //---------------------
    // EMPTY BODY
    //---------------------
}
