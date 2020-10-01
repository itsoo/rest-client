package com.cupshe.restclient;

import com.cupshe.army.knife.Kv;
import com.cupshe.restclient.exception.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static com.cupshe.restclient.RestClient.LoadBalanceType;

/**
 * RestClientProxy
 *
 * @author zxy
 */
@Slf4j
public class RestClientProxy implements InvocationHandler {

    private String name;
    private String path;
    private LoadBalanceType loadBalanceType;
    private int maxAutoRetries;
    private String fallback;

    private RestTemplate client;
    private ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);

    RestClientProxy(String name, String path, LoadBalanceType loadBalanceType, int maxAutoRetries,
                    String fallback, long connectTimeout) {
        this.name = name;
        this.path = path;
        this.loadBalanceType = loadBalanceType;
        this.maxAutoRetries = maxAutoRetries;
        this.fallback = fallback;
        this.client = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .build();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        checkParamsValidity(method);
        Object body = RequestProcessor.processRequestBodyOf(method, args);
        HttpHeaders headers = getHttpHeaders(body);

        try {
            String res = sendRequestAndGetResponse(body, headers, method, args);
            if (res != null) {
                return ResponseProcessor.convertToObject(res, method);
            }

            // void
            if (method.getReturnType().isAssignableFrom(Void.TYPE)) {
                return Void.TYPE;
            }

            if (StringUtils.hasText(fallback)) {
                return FallbackInvoker.of(fallback).invoke();
            }

            throw new ConnectTimeoutException();
        } finally {
            counter.remove();
        }
    }

    private void checkParamsValidity(Method method) {
        long count = Arrays.stream(method.getParameters())
                .filter(t -> t.getAnnotation(RequestBody.class) != null)
                .count();
        Assert.isTrue(count <= 1, "@RequestBody cannot have more than one.");
    }

    private String sendRequestAndGetResponse(Object body, HttpHeaders headers, Method method, Object[] args)
            throws URISyntaxException {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        ResponseEntity<byte[]> res = null;
        String uriPath = getUriPath(path, attr.path, method, args);

        do {
            URI uri = RequestGenerator.genericUriOf(getTargetHost(name), uriPath);

            try {
                res = client.exchange(new RequestEntity<>(body, headers, attr.method, uri), byte[].class);
                if (log.isDebugEnabled()) {
                    log.debug("Rest connect success ==> [{}]", uri.toString());
                }

                break;
            } catch (ResourceAccessException e) { // Timeout
                log.error("Rest connect timeout ==> [{}]", uri.toString());
                counter.set(counter.get() + 1);
            }
        } while (counter.get() <= maxAutoRetries);

        byte[] bytes;
        return (res != null && (bytes = res.getBody()) != null) ? new String(bytes) : null;
    }

    private HttpHeaders getHttpHeaders(Object body) {
        HttpHeaders result = RequestGenerator.genericHttpHeaders();
        if (body != null) {
            result.setContentType(MediaType.APPLICATION_JSON);
        }

        return result;
    }

    private String getTargetHost(String name) {
        for (RequestCaller rm : RestClientConfigProperties.getRouters()) {
            if (rm.getName().equals(name)) {
                return rm.get(loadBalanceType);
            }
        }

        return null;
    }

    private String getUriPath(String prefix, String uri, Method method, Object[] args) {
        String result = RequestProcessor.processStandardUri(prefix, uri);
        List<Kv> pathVariables = RequestProcessor.processPathVariablesOf(method, args);
        result = RequestProcessor.processPathVariableOf(result, pathVariables);
        List<Kv> requestParams = RequestProcessor.processRequestParamsOf(method, args);
        result = RequestProcessor.processRequestParamOf(result, requestParams);
        return result;
    }
}
