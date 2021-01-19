package com.cupshe.restclient;

import com.cupshe.ak.common.BaseConstant;
import com.cupshe.ak.request.RequestTraceIdUtils;
import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.lang.PureFunction;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.*;

import static com.cupshe.restclient.RequestGenerator.*;

/**
 * WebClient
 *
 * @author zxy
 */
@PureFunction
class WebClient {

    private static final ExecutorService ES =
            new ThreadPoolExecutor(30, 200, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(400));

    private final RestClientProxy proxy;

    private final RestTemplate restTemplate;

    private final ThreadLocal<Integer> retries;

    private WebClient(RestClientProxy proxy, int connectTimeout, int readTimeout) {
        OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory();
        if (connectTimeout >= 0) {
            factory.setConnectTimeout(connectTimeout);
        }

        if (readTimeout >= 0) {
            factory.setReadTimeout(readTimeout);
        }

        this.proxy = proxy;
        this.restTemplate = new RestTemplate(factory);
        this.retries = ThreadLocal.withInitial(() -> 0);
    }

    static WebClient newInstance(RestClientProxy proxy, int connectTimeout, int readTimeout) {
        return new WebClient(proxy, connectTimeout, readTimeout);
    }

    Future<?> asyncSendRequest(Method method, Object[] args) {
        String traceId = RequestTraceIdUtils.genericTraceId();
        RequestAttributes rqs = RequestContextHolder.getRequestAttributes();

        return ES.submit(() -> {
            try {
                MDC.put(BaseConstant.MDC_SESSION_KEY, traceId);
                RequestContextHolder.setRequestAttributes(rqs);
                // response maybe of Future.class
                Object resp = sendRequest(method, args);
                if (resp instanceof Future) {
                    return ((Future<?>) resp).get();
                }

                return resp;
            } finally {
                MDC.remove(BaseConstant.MDC_SESSION_KEY);
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }

    Object sendRequest(Method method, Object[] args) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        Parameter[] mthParams = method.getParameters();
        // request body or form-data
        Object body = RequestProcessor.getRequestBodyOf(mthParams, args);
        boolean isApplicationJson = Objects.nonNull(body);
        if (!isApplicationJson && attr.isPassingParamsOfForm()) {
            body = genericFormDataOf(attr.params, mthParams, args);
        }

        String uriPath = genericUriOf(proxy.getPath(), attr, mthParams, args);
        HttpHeaders headers = genericHeaders(attr, mthParams, args, isApplicationJson);
        ResponseEntity<byte[]> resp = sendRequest(uriPath, attr.method, body, headers);
        return doResponse(resp, method, args);
    }

    private ResponseEntity<byte[]> sendRequest(String uriPath, HttpMethod method, Object body, HttpHeaders headers) {
        try {
            ResponseEntity<byte[]> resp = ResponseProcessor.REQUEST_TIMEOUT;

            do {
                try {
                    resp = sendRequest(getRequestEntity(uriPath, method, body, headers));
                    break;
                } catch (ResourceAccessException e) { // retry
                    retries.set(retries.get() + 1);
                }
            } while (retries.get() <= proxy.getMaxAutoRetries());

            return resp;
        } finally {
            retries.remove();
        }
    }

    private ResponseEntity<byte[]> sendRequest(RequestEntity<?> requestEntity) {
        try {
            Logging.info(requestEntity);
            return restTemplate.exchange(requestEntity, byte[].class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            Logging.error(e.getMessage(), requestEntity);
            throw e;
        } catch (ResourceAccessException e) { // timeout
            Logging.error(e.getMessage());
            throw e;
        }
    }

    private <T> RequestEntity<T> getRequestEntity(String uriPath, HttpMethod method, T body, HttpHeaders headers) {
        RequestCaller routers = RestClientProperties.getRouters(proxy.getName());
        if (Objects.isNull(routers)) {
            throw new NotFoundException();
        }

        String targetHost = routers.get(proxy.getLoadBalanceType());
        URI uri = genericUriOf(targetHost, uriPath);
        return new RequestEntity<>(body, headers, method, uri);
    }

    private Object doResponse(ResponseEntity<byte[]> resp, Method method, Object[] args) {
        if (ResponseProcessor.REQUEST_TIMEOUT == resp
                && proxy.nonReturnType(method)
                && proxy.nonFallbackType()) {
            throw new ConnectTimeoutException();
        }

        String json = ResponseProcessor.convertToString(resp.getBody());
        if (Objects.nonNull(json)) {
            Logging.info(json);
        }

        return proxy.callback(ResponseProcessor.convertToObject(json, method), method, args);
    }
}
