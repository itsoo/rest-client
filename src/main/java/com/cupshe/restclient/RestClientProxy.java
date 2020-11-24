package com.cupshe.restclient;

import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.util.ObjectClassUtils;
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
import java.net.URI;

import static com.cupshe.restclient.RestClient.LoadBalanceType;

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
    private final ThreadLocal<Integer> retries = ThreadLocal.withInitial(() -> 0);

    RestClientProxy(String name, String path, LoadBalanceType loadBalanceType, int maxAutoRetries,
                    Class<?> fallback, int connectTimeout, int readTimeout) {
        this.name = name;
        this.path = path;
        this.loadBalanceType = loadBalanceType;
        this.maxAutoRetries = maxAutoRetries;

        this.fallback = fallback;
        this.client = RestTemplateUtils.createRestTemplate(connectTimeout, readTimeout);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            String res = sendRequestAndGetResponse(AnnotationMethodAttribute.of(method), method, args);
            if (res != null) {
                Logging.info(res);
                return ResponseProcessor.convertToObject(res, method);
            }
            // void
            if (method.getReturnType().isAssignableFrom(void.class)) {
                return null;
            }
            // fallback
            if (ObjectClassUtils.isInconvertibleClass(fallback)) {
                return FallbackInvoker.of(fallback, method).invoke(args);
            }
            // timeout
            throw new ConnectTimeoutException();
        } finally {
            retries.remove();
        }
    }

    private String sendRequestAndGetResponse(AnnotationMethodAttribute attr, Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        Object payload = RequestProcessor.getRequestBodyOf(params, args);
        boolean isApplicationJson = (payload != null);
        if (!isApplicationJson && attr.isPassingParamsOfForm()) {
            payload = RequestGenerator.genericFormDataOf(attr.params, params, args);
        }

        HttpHeaders headers = RequestGenerator.genericHttpHeaders(attr, params, args, isApplicationJson);
        String uriPath = RequestGenerator.genericUriOf(path, attr, params, args);
        return sendRequestAndGetResponse(uriPath, attr.method, payload, headers);
    }

    private String sendRequestAndGetResponse(String uriPath, HttpMethod method, Object body, HttpHeaders headers) {
        ResponseEntity<String> re = null;
        do {
            try {
                URI uri = RequestGenerator.genericUriOf(getTargetHost(name), uriPath);
                re = sendRequestAndGetResponse(new RequestEntity<>(body, headers, method, uri));
                break;
            } catch (ResourceAccessException e) { // retry
                retries.set(retries.get() + 1);
            }
        } while (retries.get() <= maxAutoRetries);

        String result;
        return (re != null && (result = re.getBody()) != null) ? result : null;
    }

    private ResponseEntity<String> sendRequestAndGetResponse(RequestEntity<?> requestEntity) {
        try {
            Logging.info(requestEntity);
            return client.exchange(requestEntity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            Logging.error(e.getMessage(), requestEntity);
            throw e;
        } catch (ResourceAccessException e) { // Timeout
            Logging.error(e.getMessage());
            throw e;
        }
    }

    @NonNull
    private String getTargetHost(String name) {
        for (RequestCaller rm : RestClientProperties.getRouters()) {
            if (rm.getName().equals(name)) {
                return rm.get(loadBalanceType);
            }
        }

        throw new NotFoundException();
    }
}
