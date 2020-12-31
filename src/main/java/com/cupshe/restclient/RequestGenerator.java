package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.core.Kvs;
import com.cupshe.ak.request.RequestHeaderUtils;
import com.cupshe.ak.request.RequestTraceIdUtils;
import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.lang.PureFunction;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Parameter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

import static com.cupshe.restclient.RequestProcessor.*;

/**
 * RequestGenerator
 *
 * @author zxy
 */
@PureFunction
class RequestGenerator {

    private static final String PROTOCOL = "http://";

    static HttpHeaders genericHeaders() {
        HttpHeaders headers = RestClientHeaders.getDefaultHeaders();
        RestClientHeaders.resetTraceIdOfHeaders(headers);
        // default charset=utf-8
        if (CollectionUtils.isEmpty(headers.getAcceptCharset())) {
            headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
        }

        return RestClientHeaders.getFilteredHeaders(headers);
    }

    static HttpHeaders genericHeaders(
            AnnotationMethodAttribute attr, Parameter[] params, Object[] args, boolean isApplicationJson) {

        HttpHeaders result = genericHeaders();
        for (Kv kv : getRequestHeadersOf(attr.headers)) {
            result.add(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
        }
        // maybe override for request headers
        for (Kv kv : getRequestHeadersOf(params, args)) {
            result.add(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
        }
        // setting content-type
        if (isApplicationJson) {
            result.setContentType(MediaType.APPLICATION_JSON);
        } else if (attr.isPassingParamsOfForm()) {
            result.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }

        return result;
    }

    static String genericUriOf(String prefix, AnnotationMethodAttribute attr, Parameter[] params, Object[] args) {
        String result = processStandardUri(prefix, attr.path);
        result = processPathVariables(result, getPathVariablesOf(params, args));
        result = attr.isPassingParamsOfUrl()
                ? genericUriOf(result, attr.params, params, args)
                : result;
        return result;
    }

    static String genericUriOf(String uri, String[] defParams, Parameter[] mthParams, Object[] args) {
        Kvs params = new Kvs();
        params.addAll(getRequestParamsOf(mthParams, args));
        params.addAll(getRequestParamsOf(defParams));
        return processRequestParams(uri, params);
    }

    @SneakyThrows
    static URI genericUriOf(String targetHost, String path) {
        return URI.create(getUrl(targetHost, path));
    }

    static MultiValueMap<String, Object> genericFormDataOf(String[] defParams, Parameter[] mthParams, Object[] args) {
        int capacity = (mthParams.length + defParams.length) << 1;
        MultiValueMap<String, Object> result = new LinkedMultiValueMap<>(capacity);
        for (Kv kv : getRequestParamsOf(mthParams, args)) {
            result.add(kv.getKey(), kv.getValue());
        }
        for (Kv kv : getRequestParamsOf(defParams)) {
            result.add(kv.getKey(), kv.getValue());
        }

        return result;
    }

    private static String getUrl(String targetHost, String path) {
        String relTargetHost = targetHost.startsWith(PROTOCOL)
                ? targetHost
                : (PROTOCOL + targetHost);
        return relTargetHost.endsWith("/") || path.startsWith("/")
                ? (relTargetHost + path)
                : (relTargetHost + '/' + path);
    }

    /**
     * RestClientHeaders
     */
    private enum RestClientHeaders {

        CALL_SOURCE("Call-Source", "REST-CLIENT"),

        TRACE_ID("Trace-ID", null);

        //---------------------
        // PROPERTIES
        //---------------------

        private final String key;
        private final String value;

        RestClientHeaders(String key, String value) {
            this.key = key;
            this.value = value;
        }

        static HttpHeaders getDefaultHeaders() {
            HttpHeaders result = new HttpHeaders();
            for (RestClientHeaders header : RestClientHeaders.values()) {
                result.set(header.key, header.value);
            }
            // request-context headers
            for (Kv kv : RequestHeaderUtils.getRequestHeaders()) {
                result.set(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
            }

            return result;
        }

        static HttpHeaders getFilteredHeaders(HttpHeaders headers) {
            headers.remove(HttpHeaders.AUTHORIZATION);
            headers.remove(HttpHeaders.ACCEPT_ENCODING);
            headers.remove(HttpHeaders.CONTENT_TYPE);
            headers.remove(HttpHeaders.HOST);
            return headers;
        }

        static void resetTraceIdOfHeaders(HttpHeaders headers) {
            if (!RestClientHeaders.contains(headers, TRACE_ID.key)) {
                headers.set(TRACE_ID.key, RequestTraceIdUtils.genericTraceId());
            }
        }

        static boolean contains(HttpHeaders headers, String key) {
            return getHeader(headers, key).length > 0;
        }

        static String[] getHeader(HttpHeaders headers, String key) {
            return headers.getOrEmpty(key)
                    .parallelStream()
                    .filter(Objects::nonNull)
                    .toArray(String[]::new);
        }
    }
}
