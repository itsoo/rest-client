package com.cupshe.restclient;

import com.cupshe.restclient.lang.PureFunction;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RestClientProperties
 *
 * @author zxy
 */
@Component
@ConfigurationProperties(prefix = "rest-client", ignoreInvalidFields = true)
public class RestClientProperties {

    @NestedConfigurationProperty
    private static Map<String, RequestCaller> routers;

    //---------------------
    // GETTER AND SETTER
    //---------------------

    @PureFunction
    static RequestCaller getRouters(String name) {
        return routers.get(name);
    }

    public void setRouters(@NonNull List<RequestCaller> routers) {
        RestClientProperties.routers = routers
                .parallelStream()
                .collect(Collectors.toMap(RequestCaller::getName, t -> t));
    }
}
