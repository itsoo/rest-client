package com.cupshe.restclient;

import com.cupshe.ak.UuidUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static com.cupshe.ak.BaseConstant.*;

/**
 * RequestGenerator
 *
 * @author zxy
 */
class RequestGenerator {

    static HttpHeaders genericHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CALL_SOURCE_KEY, CALL_SOURCE_VALUE);
        headers.add(TRANCE_ID_KEY, genericTranceId());
        return headers;
    }

    static URI genericUriOf(String targetHost, String path) throws URISyntaxException {
        Assert.notNull(targetHost, "Param <targetHost> cannot be null.");

        String url = targetHost;
        if (!url.startsWith(PROTOCOL)) {
            url = PROTOCOL + url;
        }

        url = url.endsWith("/") || path.startsWith("/") ? url + path : url + '/' + path;
        return new URI(url);
    }

    private static String genericTranceId() {
        try {
            return Optional.ofNullable(TRANCE_ID_STORE.get()).orElse(UuidUtils.createUuid());
        } finally {
            TRANCE_ID_STORE.remove();
        }
    }
}
