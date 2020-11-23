package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.net.UuidUtils;
import com.cupshe.ak.text.StringUtils;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.Optional;

import static com.cupshe.ak.common.BaseConstant.TRACE_ID_KEY;
import static com.cupshe.ak.common.BaseConstant.TRACE_ID_STORE;
import static com.cupshe.restclient.RequestProcessor.*;

/**
 * RequestGenerator
 *
 * @author zxy
 */
class RequestGenerator {

    /*** 调用来源头部标识 */
    private static final String CALL_SOURCE_KEY = "Call-Source";

    /*** 调用来源头部标识 */
    private static final String CALL_SOURCE_VALUE = "REST-CLIENT";

    /*** HTTP 请求协议 */
    private static final String PROTOCOL = "http://";

    static HttpHeaders genericHttpHeaders() {
        HttpHeaders result = new HttpHeaders();
        result.add(CALL_SOURCE_KEY, CALL_SOURCE_VALUE);
        result.add(TRACE_ID_KEY, genericTranceId());
        return result;
    }

    static HttpHeaders genericHttpHeaders(AnnotationMethodAttribute attr, boolean isApplicationJson) {
        HttpHeaders result = genericHttpHeaders();
        for (Kv kv : getRequestHeadersOf(attr.headers)) {
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
        String result = uri;
        result = processRequestParamOf(result, getRequestParamsOf(mthParams, args));
        result = processRequestParamOf(result, getRequestParamsOf(defParams));
        return result;
    }

    @SneakyThrows
    static URI genericUriOf(String targetHost, String path) {
        Assert.notNull(targetHost, "Param 'targetHost' cannot be null.");
        String url = targetHost.startsWith(PROTOCOL) ? targetHost : (PROTOCOL + targetHost);
        url = url.endsWith("/") || path.startsWith("/") ? (url + path) : (url + '/' + path);
        return URI.create(url);
    }

    private static String genericTranceId() {
        try {
            return Optional.ofNullable(TRACE_ID_STORE.get()).orElse(UuidUtils.createUuid());
        } finally {
            TRACE_ID_STORE.remove();
        }
    }
}
