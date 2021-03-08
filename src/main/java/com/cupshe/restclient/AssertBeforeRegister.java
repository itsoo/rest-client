package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.exception.NoSupportMethodException;
import com.cupshe.restclient.lang.RestClient;
import com.cupshe.restclient.parser.PathVariableExpressionParser;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AssertBeforeRegister
 *
 * @author zxy
 */
class AssertBeforeRegister {

    private static Map<String, String> registeredBeans = new ConcurrentHashMap<>(32);

    private static final Object CLEAR_REGISTERED_BEANS_LOCK = new Object();

    static void assertSingletonRegister(String beanName, String className) {
        String repClassName = registeredBeans.computeIfAbsent(beanName, k -> className);
        if (StringUtils.isNotEquals(repClassName, className)) {
            String message = "Annotation-specified bean name '{}' for bean class [{}] conflicts with existing, " +
                    "non-compatible bean definition of same name and class [{}].";
            throw new BeanDefinitionStoreException(
                    StringUtils.getFormatString(message, beanName, className, registeredBeans.get(beanName)));
        }
    }

    static void clearCheckedRegisterCache() {
        if (Objects.nonNull(registeredBeans)) {
            synchronized (CLEAR_REGISTERED_BEANS_LOCK) {
                if (Objects.nonNull(registeredBeans)) {
                    registeredBeans.clear();
                    registeredBeans = null;
                }
            }
        }
    }

    @NonNull
    @SneakyThrows
    static RestClient assertAndGetAnnotation(String className) {
        return assertRestClientIsInterface(Class.forName(className));
    }

    @NonNull
    static RestClient assertRestClientIsInterface(Class<?> clazz) {
        assertIsTrue(clazz.isInterface(), clazz, "@RestClient component can only be interface.");
        RestClient ann = AnnotationUtils.findAnnotation(clazz, RestClient.class);
        Assert.notNull(ann, clazz.getCanonicalName() + ": Cannot found interface with @RestClient.");
        assertNameOrValueIsNotEmpty(ann, clazz);
        assertMaxAutoRetriesValue(ann, clazz);
        assertFallbackClass(ann, clazz);
        // assert all methods
        for (Method method : ReflectionUtils.getDeclaredMethods(clazz)) {
            assertRequestBodyOnlyOne(method);
            assertRequestMappingMethod(method);
            assertRequestMappingPath(method);
            assertXxxMappingOnlyOne(method);
            assertPathVariableParams(method);
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

        boolean checkClassTyped = !fallback.isInterface() && !Modifier.isAbstract(fallback.getModifiers());
        assertIsTrue(checkClassTyped, clazz, "Fallback class cannot be interface or abstract class.");

        long count = Arrays.stream(fallback.getDeclaredAnnotations())
                .filter(t -> SupportedAnnotations.isSupport(t.annotationType()))
                .count();
        String types = SupportedAnnotations.supportTypes();
        String message = StringUtils.getFormatString("Fallback annotation only supports one of [{}].", types);
        assertIsTrue(count <= 1L, clazz, message);
    }

    static void assertRequestBodyOnlyOne(Method method) {
        long count = Arrays.stream(method.getParameters())
                .filter(t -> AnnotationUtils.findAnnotation(t, RequestBody.class) != null)
                .count();
        assertIsTrue(count <= 1L, method, "@RequestBody of the method cannot have that more than one.");
    }

    static void assertRequestMappingMethod(Method method) {
        RequestMapping ann = getRequestMappingOfMethod(method);
        assertIsTrue(ann.method().length > 0, method, "@RequestMapping property 'method' cannot be empty.");
        assertIsTrue(ann.method().length == 1, method, "@RequestMapping property 'method' can only one.");
    }

    static void assertRequestMappingPath(Method method) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        assertIsTrue(attr.paths.length <= 1, method, "@RequestMapping 'path' or 'value' is only one param.");
    }

    static void assertXxxMappingOnlyOne(Method method) {
        int count = 0;
        for (Annotation ann : method.getDeclaredAnnotations()) {
            try {
                AnnotationMethodAttribute.of(ann, false);
                count++;
            } catch (NoSupportMethodException ignore) {}
        }

        assertIsTrue(count == 1, method, "@RequestMapping is required and only one.");
    }

    static void assertPathVariableParams(Method method) {
        AnnotationMethodAttribute attr = AnnotationMethodAttribute.of(method);
        long pvCnt1 = StringUtils.findSubstringCountOf(attr.path, PathVariableExpressionParser.EXPRESSION_DELIMITER_PREFIX);
        long pvCnt2 = StringUtils.findSubstringCountOf(attr.path, PathVariableExpressionParser.EXPRESSION_DELIMITER_SUFFIX);
        long mpsCnt = Arrays.stream(method.getParameters())
                .filter(t -> AnnotationUtils.findAnnotation(t, PathVariable.class) != null)
                .count();
        assertIsTrue(pvCnt1 == pvCnt2, method, "@RequestMapping 'path' format error.");
        assertIsTrue(pvCnt1 == mpsCnt, method, "Wrong params map to request @PathVariable.");
    }

    @NonNull
    private static RequestMapping getRequestMappingOfMethod(Method method) {
        RequestMapping result = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        Assert.notNull(result, method.toGenericString() + ": Cannot found anyone @RequestMapping class.");
        return result;
    }

    private static void assertIsTrue(boolean expr, Class<?> clazz, String message) {
        Assert.isTrue(expr, clazz.getCanonicalName() + ": " + message);
    }

    private static void assertIsTrue(boolean expr, Method method, String message) {
        Assert.isTrue(expr, method.toGenericString() + ": " + message);
    }
}
