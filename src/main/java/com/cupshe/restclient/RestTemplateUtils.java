package com.cupshe.restclient;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateUtils
 *
 * @author zxy
 */
class RestTemplateUtils {

    static RestTemplate createRestTemplate(int connectTimeout, int readTimeout) {
        return new RestTemplate(getClientFactory(connectTimeout, readTimeout));
    }

    private static ClientHttpRequestFactory getClientFactory(int connectTimeout, int readTimeout) {
        OkHttp3ClientHttpRequestFactory result = new OkHttp3ClientHttpRequestFactory();
        if (connectTimeout >= 0) {
            result.setConnectTimeout(connectTimeout);
        }

        if (readTimeout >= 0) {
            result.setReadTimeout(readTimeout);
        }

        return result;
    }
}
