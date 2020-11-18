package com.cupshe.restclient;

import com.cupshe.restclient.util.BeanUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * FallbackInvoker
 *
 * @author zxy
 */
class FallbackInvoker {

    private final Class<?> reference;
    private final Method method;

    private FallbackInvoker(Class<?> reference, Method method) {
        this.reference = reference;
        this.method = method;
    }

    static FallbackInvoker of(Class<?> reference, Method method) {
        assertInconvertibleValue(reference);
        return new FallbackInvoker(reference, method);
    }

    Object invoke(Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        Method fallback = reference.getDeclaredMethod(methodName, paramTypes);
        return fallback.invoke(reference.newInstance(), args);
    }

    private static void assertInconvertibleValue(Class<?> arg) {
        Assert.isTrue(BeanUtils.isInconvertibleClass(arg), "The fallback class cannot be primitive.");
    }
}
