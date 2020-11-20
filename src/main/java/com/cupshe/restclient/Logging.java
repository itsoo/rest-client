package com.cupshe.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;

/**
 * Logging
 *
 * @author zxy
 */
@Slf4j
class Logging {

    static void info(String data) {
        log.info("Rest-client response data is ===> {}", data);
    }

    static void info(RequestEntity<?> entity) {
        log.info("Rest-client request params ===> {}", entity.toString());
    }

    static void error(String message) {
        log.error("Rest-client request timeout: {}", message);
    }

    static void error(String message, RequestEntity<?> entity) {
        log.error("Rest-client failed request {} ===> {}", message, entity.getUrl());
    }
}
