package com.cupshe.restclient;

import com.cupshe.restclient.fallback.FallbackInvoker;
import com.cupshe.restclient.lb.LoadBalanceType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

/**
 * RestClientFactoryBean
 *
 * @author zxy
 */
public class RestClientFactoryBean implements FactoryBean<Object>, EnvironmentAware, ApplicationContextAware {

    private final Class<?> clazz;

    private final String name;

    private final String path;

    private final LoadBalanceType loadBalanceType;

    private final int maxAutoRetries;

    private final Class<?> fallback;

    private final int connectTimeout;

    private final int readTimeout;

    private Environment env;

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
    public void setEnvironment(@NonNull Environment env) {
        this.env = env;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext ctx) throws BeansException {
        // loaded only config properties
        ctx.getBean(RestClientProperties.class);
        // register fallback application context
        FallbackInvoker.setCtx(ctx);
    }

    @Override
    public Object getObject() {
        return Proxy.newProxyInstance(clazz.getClassLoader(), ofArray(clazz), newProxyInstance());
    }

    @Override
    public Class<?> getObjectType() {
        return clazz;
    }

    private Class<?>[] ofArray(Class<?>... args) {
        return args;
    }

    private RestClientProxy newProxyInstance() {
        return new RestClientProxy(
                name, path, loadBalanceType, maxAutoRetries, fallback, connectTimeout, readTimeout, env);
    }
}
