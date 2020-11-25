package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.net.UuidUtils;
import com.cupshe.ak.text.StringUtils;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.cupshe.ak.common.BaseConstant.*;
import static com.cupshe.restclient.RequestProcessor.*;

/**
 * RequestGenerator
 *
 * @author zxy
 */
class RequestGenerator {

    private static final String CALL_SOURCE_KEY = "Call-Source";

    private static final String CALL_SOURCE_VALUE = "REST-CLIENT";

    private static final String PROTOCOL = "http://";

    static HttpHeaders genericHttpHeaders() {
        HttpHeaders result = new HttpHeaders();
        result.add(CALL_SOURCE_KEY, CALL_SOURCE_VALUE);
        result.add(TRACE_ID_KEY, genericTranceId());
        // request context headers
        if (REQ_HEADERS_STORE.get() != null) {
            for (Kv kv : REQ_HEADERS_STORE.get()) {
                result.add(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
            }
        }

        return result;
    }

    static HttpHeaders genericHttpHeaders(
            AnnotationMethodAttribute attr, Parameter[] params, Object[] args, boolean isApplicationJson) {

        HttpHeaders result = genericHttpHeaders();
        for (Kv kv : getRequestHeadersOf(attr.headers)) {
            result.add(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
        }
        // maybe override for request headers
        for (Kv kv : getRequestHeadersOf(params, args)) {
            result.add(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
        }

        if (isApplicationJson) {
            result.setContentType(MediaType.APPLICATION_JSON);
        } else if (attr.isPassingParamsOfForm()) {
            result.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }

        return result;
    }

    static String genericUriOf(String prefix, AnnotationMethodAttribute attr, Parameter[] params, Object[] args) {
        String result = processStandardUri(prefix, attr.path);
        result = processPathVariableOf(result, getPathVariablesOf(params, args));
        result = attr.isPassingParamsOfUrl()
                ? genericUriOf(result, attr.params, params, args)
                : result;
        return result;
    }

    static String genericUriOf(String uri, String[] defParams, Parameter[] mthParams, Object[] args) {
        // space for time
        int initialCapacity = (mthParams.length + defParams.length) << 1;
        List<Kv> params = new ArrayList<>(initialCapacity);
        params.addAll(getRequestParamsOf(mthParams, args));
        params.addAll(getRequestParamsOf(defParams));
        return processRequestParamOf(uri, params);
    }

    static MultiValueMap<String, Object> genericFormDataOf(String[] defParams, Parameter[] mthParams, Object[] args) {
        MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
        for (Kv kv : getRequestParamsOf(mthParams, args)) {
            result.add(kv.getKey(), kv.getValue());
        }
        for (Kv kv : getRequestParamsOf(defParams)) {
            result.add(kv.getKey(), kv.getValue());
        }

        return result;
    }

    @SneakyThrows
    static URI genericUriOf(String targetHost, String path) {
        return URI.create(getUrl(targetHost, path));
    }

    @NonNull
    private static String getUrl(String targetHost, String path) {
        String url = targetHost.startsWith(PROTOCOL)
                ? targetHost
                : PROTOCOL + targetHost;
        return (url.endsWith("/") || path.startsWith("/"))
                ? url + path
                : url + '/' + path;
    }

    @NonNull
    private static String genericTranceId() {
        try {
            return Optional.ofNullable(TRACE_ID_STORE.get()).orElse(UuidUtils.createUuid());
        } finally {
            TRACE_ID_STORE.remove();
        }
    }
}
