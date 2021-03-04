package com.cupshe.restclient;

import com.cupshe.ak.common.BaseConstant;
import com.cupshe.ak.core.Kv;
import com.cupshe.ak.core.Kvs;
import com.cupshe.ak.request.RequestHeaderUtils;
import com.cupshe.ak.request.RequestTraceIdUtils;
import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.lang.PureFunction;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
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

    static RequestEntity<?> genericRequestEntity(
            String targetHost, String uriPath, AnnotationMethodAttribute attr, Parameter[] params, Object[] args) {

        Object body = RequestProcessor.getRequestBodyOf(params, args);
        boolean isApplicationJson = Objects.nonNull(body);
        if (!isApplicationJson && attr.isPassingParamsOfForm()) {
            // form-data
            int capacity = (params.length + attr.params.length) << 1;
            MultiValueMap<String, Object> result = new LinkedMultiValueMap<>(capacity);
            for (Kv kv : getRequestParamsOf(params, args)) {
                result.add(kv.getKey(), kv.getValue());
            }
            for (Kv kv : getRequestParamsOf(attr.params)) {
                result.add(kv.getKey(), kv.getValue());
            }

            body = result;
        }

        HttpHeaders headers = genericHeaders(attr, params, args, isApplicationJson);
        return new RequestEntity<>(body, headers, attr.method, genericUriOf(attr.httpsSupported, targetHost, uriPath));
    }

    private static HttpHeaders genericHeaders() {
        HttpHeaders headers = RestClientHeaders.getDefaultHeaders();
        RestClientHeaders.resetTraceIdOfHeaders(headers);
        // default charset=utf-8
        if (CollectionUtils.isEmpty(headers.getAcceptCharset())) {
            headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
        }

        return RestClientHeaders.getFilteredHeaders(headers);
    }

    private static HttpHeaders genericHeaders(
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
        if (attr.isPassingParamsOfUrl()) {
            Kvs kvs = new Kvs();
            kvs.addAll(getRequestParamsOf(params, args));
            kvs.addAll(getRequestParamsOf(attr.params));
            result = processRequestParams(result, kvs);
        }

        return result;
    }

    @SneakyThrows
    private static URI genericUriOf(boolean httpsSupported, String targetHost, String path) {
        String absHost = getAbsTargetHost(httpsSupported, targetHost);
        String requestUrl = path.startsWith("/") ? (absHost + path) : (absHost + '/' + path);
        return URI.create(requestUrl);
    }

    private static String getAbsTargetHost(boolean httpsSupported, String targetHost) {
        String protocol = httpsSupported ? Protocols.HTTPS : Protocols.HTTP;
        String absHost = targetHost.startsWith(protocol) ? targetHost : (protocol + targetHost);
        return absHost.endsWith("/") ? absHost.substring(0, absHost.length() - 1) : absHost;
    }

    /**
     * Protocols
     */
    private interface Protocols {

        /*** http */
        String HTTP = "http://";

        /*** https */
        String HTTPS = "https://";
    }

    /**
     * RestClientHeaders
     */
    private enum RestClientHeaders {

        /*** call-source */
        CALL_SOURCE("X-Call-Source", "REST-CLIENT"),

        /*** trace-id */
        TRACE_ID(BaseConstant.TRACE_ID_KEY, null);

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
            for (RestClientHeaders header : values()) {
                result.set(header.key, header.value);
            }
            // request-context headers
            for (Kv kv : RequestHeaderUtils.getRequestHeaders()) {
                result.set(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
            }

            return result;
        }

        static HttpHeaders getFilteredHeaders(HttpHeaders headers) {
            // reset content-type
            headers.remove(HttpHeaders.CONTENT_TYPE);
            // filter headers
            for (String rh : RestClientProperties.getFilterHeaders()) {
                headers.remove(rh);
            }

            return headers;
        }

        static void resetTraceIdOfHeaders(HttpHeaders headers) {
            if (!contains(headers, TRACE_ID.key)) {
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
