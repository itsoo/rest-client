package com.cupshe.restclient;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * AnnotationMethodAttribute
 *
 * @author zxy
 */
class AnnotationMethodAttribute {

    String path;
    HttpMethod method;

    private AnnotationMethodAttribute(String[] path, HttpMethod method) {
        Assert.isTrue(path.length == 1, "@RequestMapping value is wrong (only one parameter).");
        this.path = path[0];
        this.method = method;
    }

    static AnnotationMethodAttribute of(Method method) {
        return of(getMethodAnnotation(method));
    }

    private static AnnotationMethodAttribute of(Annotation annotation) {
        if (annotation.annotationType().isAssignableFrom(GetMapping.class)) {
            return of(((GetMapping) annotation).value(), HttpMethod.GET);
        } else if (annotation.annotationType().isAssignableFrom(PostMapping.class)) {
            return of(((PostMapping) annotation).value(), HttpMethod.POST);
        } else if (annotation.annotationType().isAssignableFrom(PutMapping.class)) {
            return of(((PutMapping) annotation).value(), HttpMethod.PUT);
        } else if (annotation.annotationType().isAssignableFrom(PatchMapping.class)) {
            return of(((PatchMapping) annotation).value(), HttpMethod.PATCH);
        } else if (annotation.annotationType().isAssignableFrom(DeleteMapping.class)) {
            return of(((DeleteMapping) annotation).value(), HttpMethod.DELETE);
        } else if (annotation.annotationType().isAssignableFrom(RequestMapping.class)) {
            RequestMethod[] m = ((RequestMapping) annotation).method();
            Assert.isTrue(m.length == 1, "@RequestMapping method is wrong (only one parameter).");
            HttpMethod method = HttpMethod.resolve(m[0].name());
            return of(((RequestMapping) annotation).value(), method == null ? HttpMethod.GET : method);
        }

        // GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
        throw new IllegalArgumentException("No support type of @RequestMapping.");
    }

    private static AnnotationMethodAttribute of(String[] path, HttpMethod method) {
        return new AnnotationMethodAttribute(path, method);
    }

    private static Annotation getMethodAnnotation(Method method) {
        Annotation[] annotations = method.getAnnotations();
        if (annotations == null || annotations.length != 1) {
            throw new IllegalArgumentException("@RequestMapping required and only one.");
        }

        return annotations[0];
    }
}
