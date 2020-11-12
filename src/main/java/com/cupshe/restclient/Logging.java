package com.cupshe.restclient;

import com.cupshe.restclient.exception.AbstractHttpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;

/**
 * Logging
 *
 * @author zxy
 */
@Slf4j
class Logging {

    static void debug(String data) {
        if (log.isDebugEnabled()) {
            log.debug("Rest-client response data is ===> {}", data);
        }
    }

    static void debug(RequestEntity<?> entity) {
        if (log.isDebugEnabled()) {
            log.debug("Rest-client request params ===> {}", entity.toString());
        }
    }

    static void error(String message) {
        log.error("Rest-client request timeout: {}", message);
    }

    static void error(AbstractHttpException e, RequestEntity<?> entity) {
        log.error("Rest-client failed request {} {} ===> {}", e.getStatusCode(), e.getMessage(), entity.getUrl());
    }
}
