package com.cupshe.restclient;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import static com.cupshe.restclient.RestClient.LoadBalanceType;

/**
 * RestClientFactoryBean
 *
 * @author zxy
 */
public class RestClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private final Class<?> clazz;
    private final String name;
    private final String path;
    private final LoadBalanceType loadBalanceType;
    private final int maxAutoRetries;
    private final String fallback;
    private final int connectTimeout;
    private final int readTimeout;

    private ApplicationContext applicationContext;

    public RestClientFactoryBean(Class<?> clazz, String name, String path, LoadBalanceType loadBalanceType,
                                 int maxAutoRetries, String fallback, int connectTimeout, int readTimeout) {
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object getObject() {
        // loaded only config properties
        applicationContext.getBean(RestClientProperties.class);
        return Proxy.newProxyInstance(this.clazz.getClassLoader(), ofArray(this.clazz),
                new RestClientProxy(name, path, loadBalanceType, maxAutoRetries, fallback, connectTimeout, readTimeout));
    }

    @Override
    public Class<?> getObjectType() {
        return clazz;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.clazz, "Proxy class cannot be null.");
    }

    private Class<?>[] ofArray(Class<?>... args) {
        return args;
    }
}
