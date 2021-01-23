package com.cupshe.restclient.factory;

import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * ClientRestTemplateFactory
 *
 * @author zxy
 */
public class ClientRestTemplateFactory {

    public static RestTemplate getRestTemplate(int connectTimeout, int readTimeout) {
        OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory();
        if (connectTimeout >= 0) {
            factory.setConnectTimeout(connectTimeout);
        }

        if (readTimeout >= 0) {
            factory.setReadTimeout(readTimeout);
        }

        return new RestTemplate(factory);
    }
}
