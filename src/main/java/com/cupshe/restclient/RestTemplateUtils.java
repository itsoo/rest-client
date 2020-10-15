package com.cupshe.restclient;

import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateUtils
 *
 * @author zxy
 */
class RestTemplateUtils {

    static RestTemplate createRestTemplate(int connectTimeout, int readTimeout) {
        OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
