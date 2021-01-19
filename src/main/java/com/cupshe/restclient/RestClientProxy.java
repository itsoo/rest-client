package com.cupshe.restclient;

import com.cupshe.restclient.exception.ConnectTimeoutException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Future;

import static com.cupshe.restclient.lang.RestClient.LoadBalanceType;

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

    private final WebClient client;

    RestClientProxy(String name, String path, LoadBalanceType loadBalanceType, int maxAutoRetries,
                    Class<?> fallback, int connectTimeout, int readTimeout) {

        this.name = name;
        this.path = path;
        this.loadBalanceType = loadBalanceType;
        this.maxAutoRetries = maxAutoRetries;
        this.fallback = fallback;
        this.client = WebClient.newInstance(this, connectTimeout, readTimeout);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return Future.class.isAssignableFrom(method.getReturnType())
                ? client.asyncSendRequest(method, args)
                : client.sendRequest(method, args);
    }

    @SneakyThrows
    public Object callback(Object resp, Method method, Object[] args) {
        if (Objects.nonNull(resp)) {
            return resp;
        }

        // void
        if (nonReturnType(method)) {
            return null;
        }

        // fallback
        if (!nonFallbackType()) {
            return FallbackInvoker.of(fallback, method).invoke(args);
        }

        // timeout
        throw new ConnectTimeoutException();
    }

    public boolean nonReturnType(Method method) {
        return method.getReturnType() == void.class;
    }

    public boolean nonFallbackType() {
        return fallback == void.class;
    }
}
