package com.cupshe.restclient;

import com.cupshe.restclient.exception.BadRequestException;
import com.cupshe.restclient.exception.ConnectTimeoutException;
import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.exception.UnauthorizedException;
import com.cupshe.restclient.util.BeanUtils;
import lombok.SneakyThrows;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
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
                Logging.info(res);
                return ResponseProcessor.convertToObject(res, method);
            } else if (method.getReturnType().isAssignableFrom(void.class)) {
                return null;
            } else if (BeanUtils.isInconvertibleClass(fallback)) {
                return FallbackInvoker.of(fallback, method).invoke(args);
            } else {
                throw new ConnectTimeoutException();
            }
        } finally {
            counter.remove();
        }
    }

    private void checkParamsValidity(Method method) {
        long count = Arrays.stream(method.getParameters())
                .filter(t -> t.getDeclaredAnnotation(RequestBody.class) != null)
                .count();
        Assert.isTrue(count <= 1L, "@RequestBody of the method cannot have more than one.");
    }

    private String sendRequestAndGetResponse(AnnotationMethodAttribute attr, Method method, Object[] args) {
        Object body = processRequestBodyOf(method.getParameters(), args);
        boolean isApplicationJsonType = body != null;
        HttpHeaders headers = getHttpHeaders(attr, isApplicationJsonType);
        if (!isApplicationJsonType && attr.isPassingParamsOfForm()) {
            body = RequestProcessor.convertObjectsToMultiValueMap(args);
        }

        String uriPath = getUriPath(path, attr, method.getParameters(), args);
        return sendRequestAndGetResponse(uriPath, attr.method, body, headers);
    }

    @SneakyThrows
    private String sendRequestAndGetResponse(String uriPath, HttpMethod method, Object body, HttpHeaders headers) {
        ResponseEntity<byte[]> res = null;

        do {
            try {
                URI uri = RequestGenerator.genericUriOf(getTargetHost(name), uriPath);
                res = sendRequestAndGetResponse(new RequestEntity<>(body, headers, method, uri));
                break;
            } catch (RestClientException e) {
                counter.set(counter.get() + 1);
            }
        } while (counter.get() <= maxAutoRetries);

        byte[] b;
        return (res != null && (b = res.getBody()) != null) ? new String(b, StandardCharsets.UTF_8) : null;
    }

    private ResponseEntity<byte[]> sendRequestAndGetResponse(RequestEntity<?> requestEntity) {
        try {
            Logging.info(requestEntity);
            return client.exchange(requestEntity, byte[].class);
        } catch (HttpClientErrorException.BadRequest e) {
            Logging.error(new BadRequestException(), requestEntity);
            throw e;
        } catch (HttpClientErrorException.Unauthorized e) {
            Logging.error(new UnauthorizedException(), requestEntity);
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            Logging.error(new NotFoundException(), requestEntity);
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

        result.setContentType(isApplicationJsonType
                ? MediaType.APPLICATION_JSON
                : MediaType.APPLICATION_FORM_URLENCODED);
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
