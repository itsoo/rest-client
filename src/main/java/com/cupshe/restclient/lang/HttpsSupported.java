package com.cupshe.restclient.lang;

import java.lang.annotation.*;

/**
 * HttpsSupported
 * <p>Marking this annotation will send an HTTPS request.
 *
 * @author zxy
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpsSupported {
    //---------------------
    // EMPTY BODY
    //---------------------
}
