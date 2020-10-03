package com.cupshe.restclient;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * RestClientProperties
 *
 * @author zxy
 */
@Component
@ConfigurationProperties(prefix = "rest-client", ignoreInvalidFields = true)
public class RestClientProperties {

    @NestedConfigurationProperty
    private static List<RequestCaller> routers;

    // ~ getter and setter ~ //

    static List<RequestCaller> getRouters() {
        return Collections.unmodifiableList(routers);
    }

    public void setRouters(List<RequestCaller> routers) {
        RestClientProperties.routers = routers;
    }
}
