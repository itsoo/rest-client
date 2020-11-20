package com.cupshe.restclient;

import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.util.ObjectClassUtils;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;

import static com.cupshe.restclient.RequestProcessor.*;
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
    private final ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);

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
            } else if (method.getReturnType().isAssignableFrom(void.class)) {
                return null;
            } else if (ObjectClassUtils.isInconvertibleClass(fallback)) {
                return FallbackInvoker.of(fallback, method).invoke(args);
            } else {
                throw new ConnectTimeoutException();
            }
        } finally {
            counter.remove();
        }
    }

    private String sendRequestAndGetResponse(AnnotationMethodAttribute attr, Method method, Object[] args) {
        Object body = processRequestBodyOf(method.getParameters(), args);
        boolean isApplicationJsonType = body != null;
        HttpHeaders headers = getHttpHeaders(attr, isApplicationJsonType);
        if (!isApplicationJsonType && attr.isPassingParamsOfForm()) {
            body = RequestProcessor.convertObjectsToMultiValueMap(method.getParameters(), args);
        }

        String uriPath = getUriPath(path, attr, method.getParameters(), args);
        return sendRequestAndGetResponse(uriPath, attr.method, body, headers);
    }

    private String sendRequestAndGetResponse(String uriPath, HttpMethod method, Object body, HttpHeaders headers) {
        ResponseEntity<String> res = null;

        do {
            try {
                URI uri = RequestGenerator.genericUriOf(getTargetHost(name), uriPath);
                res = sendRequestAndGetResponse(new RequestEntity<>(body, headers, method, uri));
                break;
            } catch (ResourceAccessException e) {
                counter.set(counter.get() + 1);
            }
        } while (counter.get() <= maxAutoRetries);

        String result;
        return (res != null && (result = res.getBody()) != null) ? result : null;
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

    private HttpHeaders getHttpHeaders(AnnotationMethodAttribute attr, boolean isApplicationJsonType) {
        HttpHeaders result = RequestGenerator.genericHttpHeaders();
        for (String header : attr.headers) {
            String[] kv = StringUtils.split(header, "=");
            if (kv != null) {
                result.add(kv[0], kv[1]);
            }
        }

        if (isApplicationJsonType) {
            result.setContentType(MediaType.APPLICATION_JSON);
        } else if (attr.isPassingParamsOfForm()) {
            result.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
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

    private String getUriPath(String prefix, AnnotationMethodAttribute attr, Parameter[] params, Object[] args) {
        String result = processStandardUri(prefix, attr.path);
        result = processPathVariableOf(result, processPathVariablesOf(params, args));
        result = attr.isPassingParamsOfUrl() ? getUriPath(result, attr.params, params, args) : result;
        return StringUtils.trimAllWhitespace(result);
    }

    private String getUriPath(String uri, String[] defParams, Parameter[] mthParams, Object[] args) {
        String result = processRequestParamOf(uri, processRequestParamsOf(mthParams, args));
        return processParamsOfUri(result, defParams);
    }
}
