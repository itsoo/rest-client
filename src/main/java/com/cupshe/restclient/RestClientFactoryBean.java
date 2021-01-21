package com.cupshe.restclient;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import static com.cupshe.restclient.lang.RestClient.LoadBalanceType;

/**
 * RestClientFactoryBean
 *
 * @author zxy
 */
public class RestClientFactoryBean implements FactoryBean<Object>, ApplicationContextAware {

    private final Class<?> clazz;

    private final String name;

    private final String path;

    private final LoadBalanceType loadBalanceType;

    private final int maxAutoRetries;

    private final Class<?> fallback;

    private final int connectTimeout;

    private final int readTimeout;

    private ApplicationContext applicationContext;

    public RestClientFactoryBean(Class<?> clazz, String name, String path, LoadBalanceType loadBalanceType,
                                 int maxAutoRetries, Class<?> fallback, int connectTimeout, int readTimeout) {

        this.clazz = clazz;
        this.name = name;
        this.path = path;
        this.loadBalanceType = loadBalanceType;
        this.maxAutoRetries = maxAutoRetries;
        this.fallback = fallback;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        // register fallback application context
        FallbackInvoker.setApplicationContext((this.applicationContext = applicationContext));
    }

    @Override
    public Object getObject() {
        // loaded only config properties
        applicationContext.getBean(RestClientProperties.class);
        return Proxy.newProxyInstance(clazz.getClassLoader(), ofArray(clazz),
                new RestClientProxy(name, path, loadBalanceType, maxAutoRetries, fallback, connectTimeout, readTimeout));
    }

    @Override
    public Class<?> getObjectType() {
        return clazz;
    }

    @NonNull
    private Class<?>[] ofArray(Class<?>... args) {
        return args;
    }
}
