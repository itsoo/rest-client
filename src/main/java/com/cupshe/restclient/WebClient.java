package com.cupshe.restclient;

import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.lang.PureFunction;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cupshe.restclient.lang.RestClient.LoadBalanceType;

/**
 * WebClient
 * <ul>
 *   <li>Send request and process response</li>
 *   <li>Timeout policy</li>
 *   <li>Logging</li>
 *   <li>Service discovery and load balancing</li>
 * </ul>
 *
 * @author zxy
 */
@PureFunction
class WebClient {

    private static final ExecutorService EXECUTOR =
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
        return AsyncRequestTemplate.asyncCallback(() -> {
            Object resp = sendRequest(method, args);
            return (resp instanceof Future) ? ((Future<?>) resp).get() : resp;
        }, EXECUTOR);
    }

    Object sendRequest(Method method, Object[] args) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        Parameter[] mthParams = method.getParameters();
        // request body or form-data
        Object body = RequestProcessor.getRequestBodyOf(mthParams, args);
        boolean isApplicationJson = Objects.nonNull(body);
        if (!isApplicationJson && attr.isPassingParamsOfForm()) {
            body = RequestGenerator.genericFormDataOf(attr.params, mthParams, args);
        }

        String uriPath = RequestGenerator.genericUriOf(proxy.getPath(), attr, mthParams, args);
        HttpHeaders headers = RequestGenerator.genericHeaders(attr, mthParams, args, isApplicationJson);
        return doResponse(sendRequest(uriPath, attr.method, body, headers), method, args);
    }

    private ResponseEntity<byte[]> sendRequest(String uriPath, HttpMethod method, Object body, HttpHeaders headers) {
        try {
            ResponseEntity<byte[]> result = ResponseProcessor.REQUEST_TIMEOUT;

            do {
                try {
                    result = sendRequest(getRequestEntity(uriPath, method, body, headers));
                    break;
                } catch (ResourceAccessException e) { // retry
                    retries.set(retries.get() + 1);
                }
            } while (retries.get() <= proxy.getMaxAutoRetries());

            return result;
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
        URI requestUrl = RequestGenerator.genericUriOf(targetHost, uriPath);
        return new RequestEntity<>(body, headers, method, requestUrl);
    }

    private Object doResponse(ResponseEntity<byte[]> resp, Method method, Object[] args) {
        if (ResponseProcessor.REQUEST_TIMEOUT == resp && proxy.nonFallbackType()) {
            throw new ConnectTimeoutException();
        }

        String json = ResponseProcessor.convertToString(resp.getBody());
        if (Objects.nonNull(json)) {
            Logging.info(json);
        }

        return proxy.callback(ResponseProcessor.convertToObject(json, method), method, args);
    }

    /**
     * RequestCaller
     * <p>Built in service list to achieve load balancing.
     */
    @Data
    static class RequestCaller {

        private String name;

        private List<String> services;

        private final AbstractCaller roundRobin;

        private final AbstractCaller random;

        private RequestCaller() {
            roundRobin = new RoundRobinCaller();
            random = new RandomCaller();
        }

        @PureFunction
        String get(LoadBalanceType loadBalanceType) {
            int i = getCall(loadBalanceType).index();
            if (i == -1) {
                throw new NotFoundException();
            }

            return services.get(i);
        }

        private AbstractCaller getCall(LoadBalanceType loadBalanceType) {
            return LoadBalanceType.R.equals(loadBalanceType) ? random : roundRobin;
        }

        private abstract static class AbstractCaller {

            /**
             * Get next service of service-list
             *
             * @return int
             */
            abstract int index();
        }

        private class RoundRobinCaller extends AbstractCaller {

            private final AtomicInteger i = new AtomicInteger(0);

            @Override
            int index() {
                if (CollectionUtils.isEmpty(services)) {
                    return -1;
                }

                int curr, next, size = services.size();

                do {
                    curr = i.get();
                    next = curr >= size - 1 ? 0 : curr + 1;
                } while (!i.compareAndSet(curr, next));

                return curr;
            }
        }

        private class RandomCaller extends AbstractCaller {

            @Override
            int index() {
                return CollectionUtils.isEmpty(services) ? -1 : new Random().nextInt(services.size());
            }
        }
    }
}
