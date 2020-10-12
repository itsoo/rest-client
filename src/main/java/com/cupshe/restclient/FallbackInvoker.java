package com.cupshe.restclient;

import lombok.SneakyThrows;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * FallbackInvoker
 *
 * @author zxy
 */
class FallbackInvoker {

    private Class<?> className;
    private String methodName;

    private FallbackInvoker(Class<?> className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    static FallbackInvoker of(String reference) {
        assertInconvertibleValue(reference);
        Class<?> className = processClassNameOf(reference);
        String methodName = processMethodNameOf(reference);
        return new FallbackInvoker(className, methodName);
    }

    Object invoke() throws Throwable {
        Object target = this.className.newInstance();
        Method method = this.className.getDeclaredMethod(this.methodName);
        return method.invoke(target);
    }

    private static void assertInconvertibleValue(String arg) {
        Assert.isTrue(arg.lastIndexOf('#') != -1 && arg.charAt(0) == '@',
                "Fallback value must like '@com.examples.Demo#abc', (@FullyQualified#MethodName).");
    }

    @SneakyThrows
    private static Class<?> processClassNameOf(String reference) {
        return Class.forName(reference.substring(1, reference.lastIndexOf('#')));
    }

    private static String processMethodNameOf(String reference) {
        return reference.substring(reference.lastIndexOf('#') + 1);
    }
}
