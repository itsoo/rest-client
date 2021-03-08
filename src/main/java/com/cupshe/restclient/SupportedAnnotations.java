package com.cupshe.restclient;

import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.restclient.fallback.Fallback;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SupportedAnnotations
 *
 * @author zxy
 */
public class SupportedAnnotations {

    private static final Set<Class<?>> ALL_TYPES = Collections.unmodifiableSet(allTypes());

    public static boolean isSupport(Class<?> clazz) {
        return ALL_TYPES.contains(clazz);
    }

    public static String getValue(Annotation annotation) {
        return isSupport(annotation.annotationType())
                ? (String) ObjectClasses.getValueByMethodName("value", annotation)
                : null;
    }

    public static String supportTypes() {
        return ALL_TYPES
                .stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", "));
    }

    private static Set<Class<?>> allTypes() {
        Set<Class<?>> result = new LinkedHashSet<>(8);
        result.add(Fallback.class);
        result.add(Component.class);
        result.add(Service.class);
        result.add(Repository.class);
        return result;
    }
}
