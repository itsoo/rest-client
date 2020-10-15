package com.cupshe.restclient;

import com.cupshe.ak.Kv;
import com.cupshe.restclient.exception.ConnectTimeoutException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.cupshe.restclient.RequestProcessor.*;
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
                    String fallback, int connectTimeout, int readTimeout) {
        this.name = name;
        this.path = path;
        this.loadBalanceType = loadBalanceType;
        this.maxAutoRetries = maxAutoRetries;
        this.fallback = fallback;
        this.client = RestTemplateUtils.createRestTemplate(connectTimeout, readTimeout);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        checkParamsValidity(method);

        try {
            String res = sendRequestAndGetResponse(AnnotationMethodAttribute.of(method), method, args);
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

    private String sendRequestAndGetResponse(AnnotationMethodAttribute attr, Method method, Object[] args) {
        String uriPath = getUriPath(path, attr.path, attr.params, method.getParameters(), args);
        Object body = processRequestBodyOf(method.getParameters(), args);
        HttpHeaders headers = getHttpHeaders(attr, body);
        return sendRequestAndGetResponse(uriPath, attr.method, body, headers);
    }

    @SneakyThrows
    private String sendRequestAndGetResponse(String uriPath, HttpMethod method, Object body, HttpHeaders headers) {
        URI uri = RequestGenerator.genericUriOf(getTargetHost(name), uriPath);
        ResponseEntity<byte[]> res = sendRequestAndGetResponse(new RequestEntity<>(body, headers, method, uri));
        byte[] bytes;
        return (res != null && (bytes = res.getBody()) != null) ? new String(bytes) : null;
    }

    private ResponseEntity<byte[]> sendRequestAndGetResponse(RequestEntity<?> requestEntity) {
        do {
            try {
                return client.exchange(requestEntity, byte[].class);
            } catch (ResourceAccessException e) { // Timeout
                log.error(e.getMessage());
                counter.set(counter.get() + 1);
            }
        } while (counter.get() <= maxAutoRetries);

        return null;
    }

    private HttpHeaders getHttpHeaders(AnnotationMethodAttribute attr, Object body) {
        HttpHeaders result = RequestGenerator.genericHttpHeaders();
        for (String header : attr.headers) {
            String[] kv = StringUtils.split(header, "=");
            if (kv != null) {
                result.add(kv[0], kv[1]);
            }
        }

        if (body != null) {
            result.setContentType(MediaType.APPLICATION_JSON);
        }

        return result;
    }

    private String getTargetHost(String name) {
        for (RequestCaller rm : RestClientProperties.getRouters()) {
            if (rm.getName().equals(name)) {
                return rm.get(loadBalanceType);
            }
        }

        return null;
    }

    private String getUriPath(String prefix, String uri, String[] defParams, Parameter[] params, Object[] args) {
        String result = processStandardUri(prefix, uri);
        List<Kv> pathVariables = processPathVariablesOf(params, args);
        result = processPathVariableOf(result, pathVariables);
        List<Kv> requestParams = processRequestParamsOf(params, args);
        result = processRequestParamOf(result, requestParams);
        return processParamsOfUri(result, defParams);
    }
}
