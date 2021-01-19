package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.lang.PureFunction;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
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

    private static final Map<Class<?>, Object> INSTANCES_CACHE = new ConcurrentHashMap<>(32);

    private FallbackInvoker(Class<?> reference, Method method) {
        this.reference = reference;
        this.method = method;
    }

    static void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        if (Objects.isNull(FallbackInvoker.applicationContext)) {
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
        return ReflectionUtils.invokeMethod(fallback, getInstance(reference), args);
    }

    private Object getInstance(Class<?> clazz) {
        return INSTANCES_CACHE.computeIfAbsent(clazz, k -> new FallbackInstance(k).getInstance());
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
            instance = Objects.isNull(applicationContext)
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
