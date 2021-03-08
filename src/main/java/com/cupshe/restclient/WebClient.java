package com.cupshe.restclient;

import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.factory.ClientRestTemplateFactory;
import com.cupshe.restclient.factory.ThreadPoolExecutorFactory;
import com.cupshe.restclient.lb.LoadBalanceType;
import com.cupshe.restclient.lb.LoadBalancer;
import com.cupshe.restclient.lb.RandomLoadBalancer;
import com.cupshe.restclient.lb.RoundRobinLoadBalancer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.cupshe.restclient.ResponseProcessor.*;

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
@Slf4j
class WebClient {

    private static final ExecutorService EXECUTOR = ThreadPoolExecutorFactory.getThreadPoolExecutor();

    private final RestClientProxy proxy;

    private final RestTemplate restTemplate;

    private final ThreadLocal<Integer> retries;

    private WebClient(RestClientProxy proxy, int connectTimeout, int readTimeout) {
        this.proxy = proxy;
        this.restTemplate = ClientRestTemplateFactory.getRestTemplate(connectTimeout, readTimeout);
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
        AnnotationMethodAttribute attr = AnnotationMethodAttribute
                .of(method)
                .parse(proxy.getEnv());
        ResponseEntity<byte[]> resp = doRequest(attr, method.getParameters(), args);
        return doResponse(resp, method, args);
    }

    private ResponseEntity<byte[]> doRequest(AnnotationMethodAttribute attr, Parameter[] params, Object[] args) {
        try {
            ResponseEntity<byte[]> result = REQUEST_TIMEOUT;

            do {
                RequestEntity<?> requestEntity = getRequestEntity(attr, params, args);
                try {
                    log.info("Rest-client request params ===> {}", requestEntity.toString());
                    result = restTemplate.exchange(requestEntity, byte[].class);
                    break;
                } catch (ResourceAccessException e) { // timeout
                    log.error("Rest-client request timeout: {}", e.getMessage());
                    retries.set(retries.get() + 1);
                } catch (Exception e) {
                    log.error("Rest-client failed request {} ===> {}", e.getMessage(), requestEntity.toString());
                    throw e;
                }
            } while (retries.get() <= proxy.getMaxAutoRetries());

            return result;
        } finally {
            retries.remove();
        }
    }

    private RequestEntity<?> getRequestEntity(AnnotationMethodAttribute attr, Parameter[] params, Object[] args) {
        RequestCaller routers = RestClientProperties.getRouters(proxy.getName());
        if (Objects.isNull(routers)) {
            throw new NotFoundException();
        }

        String targetHost = routers.get(proxy.getLoadBalanceType());
        String uriPath = RequestGenerator.genericUriOf(proxy.getPath(), attr, params, args);
        return RequestGenerator.genericRequestEntity(targetHost, uriPath, attr, params, args);
    }

    private Object doResponse(ResponseEntity<byte[]> resp, Method method, Object[] args) {
        if (REQUEST_TIMEOUT == resp && proxy.nonFallbackType()) {
            throw new ConnectTimeoutException();
        }

        // process response body
        String json = Objects.nonNull(resp.getBody()) ? convertToString(resp.getBody()) : null;
        if (Objects.nonNull(json)) {
            log.info("Rest-client response data is ===> {}", json);
        }

        // callback policy
        Object data = Objects.nonNull(json) ? convertToObject(json, method) : null;
        return proxy.callback(data, method, args);
    }

    /**
     * RequestCaller
     * <p>Built in service list to achieve load balancing.
     */
    @Data
    static class RequestCaller {

        private String name;

        private List<String> services;

        private LoadBalancer roundRobin;

        private LoadBalancer random;

        @SuppressWarnings("unused")
        public void setServices(List<String> services) {
            this.services = services;
            this.roundRobin = new RoundRobinLoadBalancer(services);
            this.random = new RandomLoadBalancer(services);
        }

        String get(LoadBalanceType lp) {
            return getLoadBalancer(lp).next();
        }

        private LoadBalancer getLoadBalancer(LoadBalanceType lbt) {
            return LoadBalanceType.R.equals(lbt) ? random : roundRobin;
        }
    }
}
