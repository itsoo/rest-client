package com.cupshe.restclient;

import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.restclient.lang.Fallback;
import com.cupshe.restclient.lang.PureFunction;
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
@PureFunction
class SupportedAnnotations {

    private static final Set<Class<?>> ALL_TYPES = Collections.unmodifiableSet(allTypes());

    static boolean isSupport(Class<?> clazz) {
        return ALL_TYPES.contains(clazz);
    }

    static String getValue(Annotation annotation) {
        if (isSupport(annotation.annotationType())) {
            return (String) ObjectClasses.getValueByMethodName("value", annotation);
        }

        return null;
    }

    static String supportTypes() {
        return ALL_TYPES
                .parallelStream()
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
