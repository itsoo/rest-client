package com.cupshe.restclient.fallback;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.SupportedAnnotations;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * FallbackInstance
 *
 * @author zxy
 */
public class FallbackInstance {

    private String beanName;

    private Object instance;

    FallbackInstance(ApplicationContext ctx, Class<?> clazz) {
        setBeanName(clazz);
        setInstance(ctx, clazz);
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

    private void setInstance(ApplicationContext ctx, Class<?> clazz) {
        instance = Objects.isNull(ctx) ? getDefaultBean(clazz) : getBeanOrDefault(ctx, beanName, clazz);
    }

    private Object getBeanOrDefault(ApplicationContext ctx, String beanName, Class<?> clazz) {
        return ctx.containsBean(beanName) ? ctx.getBean(beanName) : getDefaultBean(clazz);
    }

    @SneakyThrows
    private Object getDefaultBean(Class<?> clazz) {
        return clazz.newInstance();
    }
}
