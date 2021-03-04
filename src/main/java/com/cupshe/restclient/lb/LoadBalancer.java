package com.cupshe.restclient.lb;

import org.springframework.lang.NonNull;

/**
 * LoadBalancer
 *
 * @author zxy
 */
public interface LoadBalancer {

    /**
     * Get next service of service-list
     *
     * @return next remote host
     */
    @NonNull
    String next();
}
