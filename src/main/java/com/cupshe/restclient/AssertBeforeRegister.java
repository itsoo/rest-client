package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.exception.NoSupportMethodException;
import com.cupshe.restclient.lang.SupportedAnnotations;
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
        RestClient ann = AnnotationUtils.findAnnotation(clazz, RestClient.class);
        assertNotNull(ann, clazz, "Cannot found interface with @RestClient.");
        assertNameOrValueIsNotEmpty(ann, clazz);
        assertMaxAutoRetriesValue(ann, clazz);
        assertFallbackClass(ann, clazz);
        // assert all methods
        for (Method method : clazz.getDeclaredMethods()) {
            assertRequestBodyOnlyOne(method, clazz);
            assertRequestMappingMethod(method, clazz);
            assertRequestMappingPath(method, clazz);
            assertXxxMappingOnlyOne(method, clazz);
            assertPathVariableParams(method, clazz);
        }

        return ann;
    }

    static void assertNameOrValueIsNotEmpty(RestClient ann, Class<?> clazz) {
        assertIsTrue(StringUtils.isNotBlank(ann.name()), clazz, "@RestClient 'name' or 'value' cannot be all empty.");
    }

    static void assertMaxAutoRetriesValue(RestClient ann, Class<?> clazz) {
        boolean checkAutoRetries = ann.maxAutoRetries() >= 0;
        assertIsTrue(checkAutoRetries, clazz, "@RestClient 'maxAutoRetries' range [0, Integer.MAX_VALUE].");
    }

    static void assertFallbackClass(RestClient ann, Class<?> clazz) {
        Class<?> fallback = ann.fallback();
        if (fallback == void.class) {
            return;
        }

        boolean checkIsSubclass = clazz.isAssignableFrom(fallback);
        assertIsTrue(checkIsSubclass, clazz, "Fallback class must implement the interface annotated by @RestClient.");
        boolean checkClassType = !fallback.isInterface()
                && !Modifier.isAbstract(fallback.getModifiers());
        assertIsTrue(checkClassType, clazz, "Fallback class cannot be interface or abstract class.");

        long count = Arrays.stream(fallback.getDeclaredAnnotations())
                .parallel()
                .filter(t -> SupportedAnnotations.isSupport(t.annotationType()))
                .count();
        String types = SupportedAnnotations.supportTypes();
        String message = StringUtils.getFormatString("Fallback annotation only supports one of [{}].", types);
        assertIsTrue(count <= 1L, clazz, message);
    }

    static void assertRequestBodyOnlyOne(Method method, Class<?> clazz) {
        long count = Arrays.stream(method.getParameters())
                .parallel()
                .filter(t -> AnnotationUtils.findAnnotation(t, RequestBody.class) != null)
                .count();
        assertIsTrue(count <= 1L, clazz, "@RequestBody of the method cannot have that more than one.");
    }

    static void assertRequestMappingMethod(Method method, Class<?> clazz) {
        RequestMapping ann = getRequestMappingOfMethod(method, clazz);
        assertIsTrue(ann.method().length > 0, clazz, "@RequestMapping property 'method' cannot be empty.");
        assertIsTrue(ann.method().length == 1, clazz, "@RequestMapping property 'method' can only one.");
    }

    static void assertRequestMappingPath(Method method, Class<?> clazz) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        assertIsTrue(attr.paths.length <= 1, clazz, "@RequestMapping 'path' or 'value' is wrong (only one param).");
    }

    static void assertXxxMappingOnlyOne(Method method, Class<?> clazz) {
        int count = 0;
        for (Annotation ann : method.getDeclaredAnnotations()) {
            try {
                AnnotationMethodAttribute.of(ann);
                count++;
            } catch (NoSupportMethodException ignore) {}
        }

        assertIsTrue(count == 1, clazz, "@RequestMapping is required and only one.");
    }

    static void assertPathVariableParams(Method method, Class<?> clazz) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        long pathParamsCount1 = StringUtils.findSubstringCountOf(attr.path, RequestProcessor.EXPRESSION_DELIMITER_PREFIX);
        long pathParamsCount2 = StringUtils.findSubstringCountOf(attr.path, RequestProcessor.EXPRESSION_DELIMITER_SUFFIX);
        long methodParamsCount = Arrays.stream(method.getParameters())
                .parallel()
                .filter(t -> AnnotationUtils.findAnnotation(t, PathVariable.class) != null)
                .count();
        assertIsTrue(pathParamsCount1 == pathParamsCount2, clazz, "Path variable expression format error.");
        assertIsTrue(pathParamsCount1 == methodParamsCount, clazz, "Wrong params map to request path variable.");
    }

    @NonNull
    private static RequestMapping getRequestMappingOfMethod(Method method, Class<?> clazz) {
        RequestMapping result = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        assertNotNull(result, clazz, "Cannot found anyone @RequestMapping class.");
        return result;
    }

    private static void assertNotNull(Object obj, Class<?> clazz, String message) {
        Assert.notNull(obj, clazz.getCanonicalName() + ": " + message);
    }

    private static void assertIsTrue(boolean exp, Class<?> clazz, String message) {
        Assert.isTrue(exp, clazz.getCanonicalName() + ": " + message);
    }
}
