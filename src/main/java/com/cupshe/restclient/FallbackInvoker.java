package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.lang.SupportedAnnotations;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * FallbackInvoker
 *
 * @author zxy
 */
class FallbackInvoker {

    private final Class<?> reference;
    private final Method method;

    private static ApplicationContext applicationContext;

    private FallbackInvoker(Class<?> reference, Method method) {
        this.reference = reference;
        this.method = method;
    }

    static void setApplicationContext(ApplicationContext applicationContext) {
        if (FallbackInvoker.applicationContext == null) {
            FallbackInvoker.applicationContext = applicationContext;
        }
    }

    static FallbackInvoker of(Class<?> reference, Method method) {
        return new FallbackInvoker(reference, method);
    }

    Object invoke(Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        Method fallback = reference.getDeclaredMethod(methodName, paramTypes);
        FallbackInstance fi = new FallbackInstance(reference);
        return fallback.invoke(fi.getInstance(), args);
    }

    /**
     * FallbackInstance
     */
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
