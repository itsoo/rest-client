package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.lang.PureFunction;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FallbackInvoker
 *
 * @author zxy
 */
class FallbackInvoker {

    private final Class<?> reference;

    private final Method method;

    private static ApplicationContext applicationContext;

    private static final Map<Class<?>, Object> INSTANCE_CACHES = new ConcurrentHashMap<>(32);

    private FallbackInvoker(Class<?> reference, Method method) {
        this.reference = reference;
        this.method = method;
    }

    static void setApplicationContext(ApplicationContext applicationContext) {
        if (FallbackInvoker.applicationContext == null) {
            FallbackInvoker.applicationContext = applicationContext;
        }
    }

    @PureFunction
    static FallbackInvoker of(Class<?> reference, Method method) {
        return new FallbackInvoker(reference, method);
    }

    @PureFunction
    Object invoke(Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        Method fallback = reference.getDeclaredMethod(methodName, paramTypes);
        return fallback.invoke(getInstance(reference), args);
    }

    private Object getInstance(Class<?> clazz) {
        // computed once
        if (!INSTANCE_CACHES.containsKey(clazz)) {
            synchronized (INSTANCE_CACHES) {
                if (!INSTANCE_CACHES.containsKey(clazz)) {
                    // computed when needed
                    INSTANCE_CACHES.put(clazz, new FallbackInstance(clazz).getInstance());
                }
            }
        }

        return INSTANCE_CACHES.get(clazz);
    }

    /**
     * FallbackInstance
     */
    @PureFunction
    private static class FallbackInstance {
        private String beanName;
        private Object instance;

        FallbackInstance(Class<?> clazz) {
            setBeanName(clazz);
            setInstance(clazz);
        }

        Object getInstance() {
            return instance;
        }

        private void setBeanName(Class<?> clazz) {
            for (Annotation ann : clazz.getDeclaredAnnotations()) {
                beanName = SupportedAnnotations.getValue(ann);
                if (StringUtils.isNotBlank(beanName)) {
                    return;
                }
            }

            beanName = ClassUtils.getShortNameAsProperty(clazz);
        }

        private void setInstance(Class<?> clazz) {
            instance = applicationContext == null
                    ? getDefaultBean(clazz)
                    : getBeanOrDefault(beanName, clazz);
        }

        private Object getBeanOrDefault(String beanName, Class<?> clazz) {
            return applicationContext.containsBean(beanName)
                    ? applicationContext.getBean(beanName)
                    : getDefaultBean(clazz);
        }

        @SneakyThrows
        private Object getDefaultBean(Class<?> clazz) {
            return clazz.newInstance();
        }
    }
}
