package com.cupshe.restclient;

import com.cupshe.restclient.exception.RepeatRouterException;
import com.cupshe.restclient.lang.PureFunction;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private boolean repeatMerged;

    @NestedConfigurationProperty
    private static Map<String, RequestCaller> routers;

    //---------------------
    // GETTER AND SETTER
    //---------------------

    @PureFunction
    static RequestCaller getRouters(String name) {
        return routers.get(name);
    }

    public void setRouters(@NonNull List<RequestCaller> params) {
        routers = new LinkedHashMap<>(params.size() << 1);
        for (RequestCaller router : params) {
            if (!isRepeatMerged() && routers.containsKey(router.getName())) {
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

    @Override
    public String toString() {
        String routersString = routers != null
                ? routers.toString()
                : "{}";
        return "RestClientProperties(" +
                routersString +
                ')';
    }
}
