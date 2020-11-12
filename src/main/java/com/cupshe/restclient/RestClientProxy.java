package com.cupshe.restclient;

import com.cupshe.ak.net.UriUtils;
import com.cupshe.restclient.exception.BadRequestException;
import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.exception.UnauthorizedException;
import lombok.SneakyThrows;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
            checkParamsValidity(method);
            String res = sendRequestAndGetResponse(AnnotationMethodAttribute.of(method), method, args);
            if (res != null) {
                Logging.debug(res);
                return ResponseProcessor.convertToObject(res, method);
            }
            // is void
            if (method.getReturnType().isAssignableFrom(Void.TYPE)) {
                return Void.TYPE;
            }
            // not primitive
            if (!fallback.isPrimitive()) {
                return FallbackInvoker.of(fallback, method).invoke(args);
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
        Assert.isTrue(count <= 1, "@RequestBody of the method cannot have more than one.");
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
        byte[] b;
        return (res != null && (b = res.getBody()) != null) ? new String(b, StandardCharsets.UTF_8) : null;
    }

    private ResponseEntity<byte[]> sendRequestAndGetResponse(RequestEntity<?> requestEntity) {
        do {
            try {
                Logging.debug(requestEntity);
                return client.exchange(requestEntity, byte[].class);
            } catch (HttpClientErrorException.BadRequest e) {
                Logging.error(new BadRequestException(), requestEntity);
            } catch (HttpClientErrorException.Unauthorized e) {
                Logging.error(new UnauthorizedException(), requestEntity);
            } catch (HttpClientErrorException.NotFound e) {
                Logging.error(new NotFoundException(), requestEntity);
            } catch (ResourceAccessException e) { // Timeout
                Logging.error(e.getMessage());
            } finally {
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
        String result = UriUtils.processStandardUri(prefix, uri);
        result = UriUtils.processPathVariableOf(result, processPathVariablesOf(params, args));
        result = UriUtils.processRequestParamOf(result, processRequestParamsOf(params, args));
        return UriUtils.processParamsOfUri(result, defParams);
    }
}
