package com.cupshe.restclient;

import com.cupshe.restclient.exception.ClientUnknownError;
import com.cupshe.restclient.lb.LoadBalanceType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * RestClientProxy
 *
 * @author zxy
 */
@Getter
public class RestClientProxy implements InvocationHandler {

    private final String name;

    private final String path;

    private final LoadBalanceType loadBalanceType;

    private final int maxAutoRetries;

    private final Class<?> fallback;

    private final Environment environment;

    private final WebClient client;

    RestClientProxy(String name, String path, LoadBalanceType loadBalanceType, int maxAutoRetries,
                    Class<?> fallback, int connectTimeout, int readTimeout, Environment environment) {

        this.name = name;
        this.path = path;
        this.loadBalanceType = loadBalanceType;
        this.maxAutoRetries = maxAutoRetries;
        this.fallback = fallback;
        this.environment = environment;
        this.client = WebClient.newInstance(this, connectTimeout, readTimeout);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return Future.class.isAssignableFrom(method.getReturnType())
                ? client.asyncSendRequest(method, args)
                : client.sendRequest(method, args);
    }

    @SneakyThrows
    Object callback(Object resp, Method method, Object[] args) {
        // response
        if (Objects.nonNull(resp)) {
            return resp;
        }

        // fallback
        if (!nonFallbackType()) {
            return FallbackInvoker.of(fallback, method).invoke(args);
        }

        // unknown error
        throw new ClientUnknownError();
    }

    boolean nonFallbackType() {
        return fallback == void.class;
    }
}
