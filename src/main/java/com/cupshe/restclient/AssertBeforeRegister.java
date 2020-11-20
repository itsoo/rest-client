package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.exception.NoSupportMethodException;
import com.cupshe.restclient.util.ObjectClassUtils;
import lombok.SneakyThrows;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AssertBeforeRegister
 *
 * @author zxy
 */
class AssertBeforeRegister {

    @NonNull
    @SneakyThrows
    static RestClient assertAndGetAnnotation(String className) {
        Class<?> clazz = Class.forName(className);
        return assertRestClientIsInterface(clazz);
    }

    @NonNull
    static RestClient assertRestClientIsInterface(Class<?> clazz) {
        Assert.isTrue(clazz.isInterface(), "@RestClient component can only be interface.");
        RestClient annotation = AnnotationUtils.findAnnotation(clazz, RestClient.class);
        Assert.notNull(annotation, "Cannot found interface with @RestClient.");
        assertNameOrValueIsNotEmpty(annotation);
        assertMaxAutoRetriesValue(annotation);
        assertFallbackClass(annotation);
        // assert all methods
        for (Method method : clazz.getDeclaredMethods()) {
            assertRequestBodyOnlyOne(method);
            assertRequestMappingMethod(method);
            assertRequestMappingPath(method);
            assertXxxMappingOnlyOne(method);
            assertPathVariableParams(method);
        }

        return annotation;
    }

    static void assertNameOrValueIsNotEmpty(RestClient annotation) {
        String serviceName = StringUtils.defaultIfBlank(annotation.name(), annotation.value());
        Assert.isTrue(StringUtils.isNotBlank(serviceName), "@RestClient 'name' or 'value' cannot empty together.");
    }

    static void assertMaxAutoRetriesValue(RestClient annotation) {
        boolean checkAutoRetries = annotation.maxAutoRetries() >= 0;
        Assert.isTrue(checkAutoRetries, "@RestClient 'maxAutoRetries' range [0, Integer.MAX_VALUE].");
    }

    static void assertFallbackClass(RestClient annotation) {
        boolean checkFallbackClass = ObjectClassUtils.isInconvertibleClass(annotation.fallback())
                || void.class.isAssignableFrom(annotation.fallback());
        Assert.isTrue(checkFallbackClass, "The fallback class cannot be primitive.");
    }

    static void assertRequestBodyOnlyOne(Method method) {
        long count = Arrays.stream(method.getParameters())
                .filter(t -> t.getDeclaredAnnotation(RequestBody.class) != null)
                .count();
        Assert.isTrue(count <= 1L, "@RequestBody of the method cannot have more than one.");
    }

    static void assertRequestMappingMethod(Method method) {
        RequestMapping annotation = getRequestMappingOfMethod(method);
        Assert.isTrue(annotation.method().length > 0, "@RequestMapping property 'method' cannot be empty.");
        Assert.isTrue(annotation.method().length == 1, "@RequestMapping property 'method' can only one.");
    }

    static void assertRequestMappingPath(Method method) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        Assert.isTrue(attr.paths.length <= 1, "@RequestMapping value is wrong (only one parameter).");
    }

    static void assertXxxMappingOnlyOne(Method method) {
        int count = 0;
        for (Annotation annotation : method.getDeclaredAnnotations()) {
            try {
                AnnotationMethodAttribute.of(annotation);
                count++;
            } catch (NoSupportMethodException ignore) {}
        }

        Assert.isTrue(count == 1, "@RequestMapping is required and only one.");
    }

    static void assertPathVariableParams(Method method) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        long pathParamsCount = StringUtils.findSubstringCountOf(attr.path, "{");
        long methodParamsCount = Arrays.stream(method.getParameters())
                .filter(t -> AnnotationUtils.findAnnotation(t, PathVariable.class) != null)
                .count();
        Assert.isTrue(pathParamsCount == methodParamsCount, "Wrong params defined by request path variables.");
    }

    @NonNull
    private static RequestMapping getRequestMappingOfMethod(Method method) {
        RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        Assert.notNull(annotation, "Cannot found anyone @RequestMapping class.");
        return annotation;
    }
}
