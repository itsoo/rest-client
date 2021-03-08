package com.cupshe.restclient.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FallbackInvoker
 *
 * @author zxy
 */
@Slf4j
public class FallbackInvoker {

    private final Class<?> reference;

    private final Method method;

    private static ApplicationContext ctx;

    private static final Map<Class<?>, Object> INSTANCES_CACHE = new ConcurrentHashMap<>(32);

    private FallbackInvoker(Class<?> reference, Method method) {
        this.reference = reference;
        this.method = method;
    }

    public static void setCtx(@NonNull ApplicationContext ctx) {
        if (Objects.isNull(FallbackInvoker.ctx)) {
            FallbackInvoker.ctx = ctx;
        }
    }

    public static FallbackInvoker of(Class<?> reference, Method method) {
        return new FallbackInvoker(reference, method);
    }

    public Object invoke(Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        Method fallback = reference.getDeclaredMethod(methodName, paramTypes);
        log.warn("Rest-client called <{}> fallback arguments: {}", fallback.toGenericString(), args);
        return ReflectionUtils.invokeMethod(fallback, getInstance(reference), args);
    }

    private Object getInstance(Class<?> clazz) {
        return INSTANCES_CACHE.computeIfAbsent(clazz, k -> new FallbackInstance(ctx, k).getInstance());
    }
}
