package com.cupshe.restclient;

import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.lang.PureFunction;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static com.cupshe.restclient.lang.RestClient.LoadBalanceType;

/**
 * RestClientProxy
 *
 * @author zxy
 */
public class RestClientProxy implements InvocationHandler {

    private final String name;
    private final String path;
    private final LoadBalanceType loadBalanceType;
    private final int maxAutoRetries;
    private final Class<?> fallback;
    private final RestTemplate client;
    private final ThreadLocal<Integer> retries;

    RestClientProxy(String name, String path, LoadBalanceType loadBalanceType, int maxAutoRetries,
                    Class<?> fallback, int connectTimeout, int readTimeout) {

        this.name = name;
        this.path = path;
        this.loadBalanceType = loadBalanceType;
        this.maxAutoRetries = maxAutoRetries;
        this.fallback = fallback;
        this.client = RestTemplateUtils.createRestTemplate(connectTimeout, readTimeout);
        this.retries = ThreadLocal.withInitial(() -> 0);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String result = sendRequestAndGetResponse(AnnotationMethodAttribute.of(method), method, args);
        if (result != null) {
            Logging.info(result);
            return ResponseProcessor.convertToObject(result, method);
        } else if (method.getReturnType() == void.class) { // void
            return null;
        } else if (fallback != void.class) { // fallback
            return FallbackInvoker.of(fallback, method).invoke(args);
        } else { // timeout
            throw new ConnectTimeoutException();
        }
    }

    @PureFunction
    private String sendRequestAndGetResponse(AnnotationMethodAttribute attr, Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        Object body = RequestProcessor.getRequestBodyOf(params, args);
        boolean isApplicationJson = (body != null);
        if (!isApplicationJson && attr.isPassingParamsOfForm()) {
            body = RequestGenerator.genericFormDataOf(attr.params, params, args);
        }

        HttpHeaders headers = RequestGenerator.genericHeaders(attr, params, args, isApplicationJson);
        String uriPath = RequestGenerator.genericUriOf(path, attr, params, args);
        return sendRequestAndGetResponse(uriPath, attr.method, body, headers);
    }

    @PureFunction
    private String sendRequestAndGetResponse(String uriPath, HttpMethod method, Object body, HttpHeaders headers) {
        try {
            ResponseEntity<String> result = null;
            do {
                try {
                    result = sendRequestAndGetResponse(new RequestEntity<>(
                            body, headers, method, RequestGenerator.genericUriOf(getTargetHost(name), uriPath)));
                    break;
                } catch (ResourceAccessException e) { // retry
                    retries.set(retries.get() + 1);
                }
            } while (retries.get() <= maxAutoRetries);
            return result != null ? result.getBody() : null;
        } finally {
            retries.remove();
        }
    }

    @PureFunction
    private ResponseEntity<String> sendRequestAndGetResponse(RequestEntity<?> requestEntity) {
        try {
            Logging.info(requestEntity);
            return client.exchange(requestEntity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            Logging.error(e.getMessage(), requestEntity);
            throw e;
        } catch (ResourceAccessException e) { // timeout
            Logging.error(e.getMessage());
            throw e;
        }
    }

    private String getTargetHost(@NonNull String name) {
        RequestCaller routers = RestClientProperties.getRouters(name);
        if (routers != null) {
            return routers.get(loadBalanceType);
        }

        throw new NotFoundException();
    }
}
