package com.cupshe.restclient;

import com.cupshe.ak.net.UuidUtils;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.Optional;

import static com.cupshe.ak.common.BaseConstant.*;

/**
 * RequestGenerator
 *
 * @author zxy
 */
class RequestGenerator {

    static HttpHeaders genericHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CALL_SOURCE_KEY, CALL_SOURCE_VALUE);
        headers.add(TRACE_ID_KEY, genericTranceId());
        return headers;
    }

    @SneakyThrows
    static URI genericUriOf(String targetHost, String path) {
        Assert.notNull(targetHost, "Param 'targetHost' cannot be null.");

        String url = targetHost;
        if (!url.startsWith(PROTOCOL)) {
            url = PROTOCOL + url;
        }

        url = url.endsWith("/") || path.startsWith("/") ? url + path : url + '/' + path;
        return new URI(url);
    }

    private static String genericTranceId() {
        try {
            return Optional.ofNullable(TRACE_ID_STORE.get()).orElse(UuidUtils.createUuid());
        } finally {
            TRACE_ID_STORE.remove();
        }
    }
}
