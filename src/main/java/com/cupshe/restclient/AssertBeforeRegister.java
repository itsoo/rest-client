package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.exception.NoSupportMethodException;
import lombok.SneakyThrows;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
        assertIsTrue(clazz.isInterface(), clazz, "@RestClient component can only be interface.");
        RestClient annotation = AnnotationUtils.findAnnotation(clazz, RestClient.class);
        assertNotNull(annotation, clazz, "Cannot found interface with @RestClient.");
        assertNameOrValueIsNotEmpty(annotation, clazz);
        assertMaxAutoRetriesValue(annotation, clazz);
        assertFallbackClass(annotation, clazz);
        // assert all methods
        for (Method method : clazz.getDeclaredMethods()) {
            assertRequestBodyOnlyOne(method, clazz);
            assertRequestMappingMethod(method, clazz);
            assertRequestMappingPath(method, clazz);
            assertXxxMappingOnlyOne(method, clazz);
            assertPathVariableParams(method, clazz);
        }

        return annotation;
    }

    static void assertNameOrValueIsNotEmpty(RestClient annotation, Class<?> clazz) {
        String serviceName = StringUtils.defaultIfBlank(annotation.name(), annotation.value());
        assertIsTrue(StringUtils.isNotBlank(serviceName), clazz, "@RestClient 'name' or 'value' cannot be all empty.");
    }

    static void assertMaxAutoRetriesValue(RestClient annotation, Class<?> clazz) {
        boolean checkAutoRetries = annotation.maxAutoRetries() >= 0;
        assertIsTrue(checkAutoRetries, clazz, "@RestClient 'maxAutoRetries' range [0, Integer.MAX_VALUE].");
    }

    static void assertFallbackClass(RestClient annotation, Class<?> clazz) {
        boolean checkIsSubclass = clazz.isAssignableFrom(annotation.fallback())
                || annotation.fallback() == void.class;
        assertIsTrue(checkIsSubclass, clazz, "Fallback class must implement the interface annotated by @RestClient.");
        boolean checkClassType = !annotation.fallback().isInterface()
                && !Modifier.isAbstract(annotation.fallback().getModifiers());
        assertIsTrue(checkClassType, clazz, "Fallback class cannot be interface or abstract class.");
    }

    static void assertRequestBodyOnlyOne(Method method, Class<?> clazz) {
        long count = Arrays.stream(method.getParameters())
                .filter(t -> t.getDeclaredAnnotation(RequestBody.class) != null)
                .count();
        assertIsTrue(count <= 1L, clazz, "@RequestBody of the method cannot have that more than one.");
    }

    static void assertRequestMappingMethod(Method method, Class<?> clazz) {
        RequestMapping annotation = getRequestMappingOfMethod(method, clazz);
        assertIsTrue(annotation.method().length > 0, clazz, "@RequestMapping property 'method' cannot be empty.");
        assertIsTrue(annotation.method().length == 1, clazz, "@RequestMapping property 'method' can only one.");
    }

    static void assertRequestMappingPath(Method method, Class<?> clazz) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        assertIsTrue(attr.paths.length <= 1, clazz, "@RequestMapping value is wrong (only one parameter).");
    }

    static void assertXxxMappingOnlyOne(Method method, Class<?> clazz) {
        int count = 0;
        for (Annotation annotation : method.getDeclaredAnnotations()) {
            try {
                AnnotationMethodAttribute.of(annotation);
                count++;
            } catch (NoSupportMethodException ignore) {}
        }

        assertIsTrue(count == 1, clazz, "@RequestMapping is required and only one.");
    }

    static void assertPathVariableParams(Method method, Class<?> clazz) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        long pathParamsCount = StringUtils.findSubstringCountOf(attr.path, "{");
        long methodParamsCount = Arrays.stream(method.getParameters())
                .filter(t -> AnnotationUtils.findAnnotation(t, PathVariable.class) != null)
                .count();
        assertIsTrue(pathParamsCount == methodParamsCount, clazz, "Wrong params defined by request path variables.");
    }

    @NonNull
    private static RequestMapping getRequestMappingOfMethod(Method method, Class<?> clazz) {
        RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        assertNotNull(annotation, clazz, "Cannot found anyone @RequestMapping class.");
        return annotation;
    }

    private static void assertNotNull(Object object, Class<?> clazz, String message) {
        Assert.notNull(object, clazz.getCanonicalName() + ": " + message);
    }

    private static void assertIsTrue(boolean expression, Class<?> clazz, String message) {
        Assert.isTrue(expression, clazz.getCanonicalName() + ": " + message);
    }
}
