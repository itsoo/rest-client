package com.cupshe.restclient;

import com.cupshe.restclient.lang.PureFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;

/**
 * Logging
 *
 * @author zxy
 */
@Slf4j
@PureFunction
class Logging {

    static void response(String arg) {
        log.info("Rest-client response data is ===> {}", arg);
    }

    static void request(RequestEntity<?> arg) {
        log.info("Rest-client request params ===> {}", arg.toString());
    }

    static void timeout(String message) {
        log.error("Rest-client request timeout: {}", message);
    }

    static void failed(String message, RequestEntity<?> arg) {
        log.error("Rest-client failed request {} ===> {}", message, arg.toString());
    }

    static void fallback(String message, Object arg) {
        log.warn("Rest-client called fallback {} ===> {}", message, arg);
    }
}
