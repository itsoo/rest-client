package com.cupshe.restclient;

import com.cupshe.restclient.exception.RepeatRouterException;
import com.cupshe.restclient.lang.PureFunction;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RestClientProperties
 *
 * @author zxy
 */
@ConfigurationProperties(prefix = "rest-client")
@SuppressWarnings("all")
public class RestClientProperties {

    @Getter
    @Setter
    private boolean mergedRouters;

    private static String[] filterHeaders;

    private static Map<String, RequestCaller> routers;

    public RestClientProperties() {
        mergedRouters = false;
        filterHeaders = new String[0];
        routers = new LinkedHashMap<>(32);
    }

    //---------------------
    // GETTER AND SETTER
    //---------------------

    @PureFunction
    static String[] getFilterHeaders() {
        return filterHeaders;
    }

    public void setFilterHeaders(String[] params) {
        if (Objects.nonNull(params)) {
            filterHeaders = params;
        }
    }

    @PureFunction
    static RequestCaller getRouters(String name) {
        return routers.get(name);
    }

    public void setRouters(@NonNull List<RequestCaller> params) {
        for (RequestCaller router : params) {
            if (!isMergedRouters() && routers.containsKey(router.getName())) {
                throw new RepeatRouterException(router.getName());
            }

            appendIfPresent(router);
        }
    }

    private void appendIfPresent(RequestCaller router) {
        RequestCaller rc = routers.computeIfAbsent(router.getName(), k -> router);
        if (rc != router) {
            rc.getServices().addAll(router.getServices());
        }
    }
}
