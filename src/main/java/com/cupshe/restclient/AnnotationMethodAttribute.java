package com.cupshe.restclient;

import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
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
    String[] headers;
    String[] params;

    private AnnotationMethodAttribute(String[] path, String[] headers, String[] params, HttpMethod method) {
        Assert.isTrue(path.length == 1, "@RequestMapping value is wrong (only one parameter).");
        this.path = path[0];
        this.headers = headers;
        this.params = params;
        this.method = method;
    }

    static AnnotationMethodAttribute of(Method method) {
        return of(getMethodAnnotation(method));
    }

    private static AnnotationMethodAttribute of(Annotation annotation) {
        if (annotation.annotationType().isAssignableFrom(GetMapping.class)) {
            return of((GetMapping) annotation);
        } else if (annotation.annotationType().isAssignableFrom(PostMapping.class)) {
            return of((PostMapping) annotation);
        } else if (annotation.annotationType().isAssignableFrom(PutMapping.class)) {
            return of((PutMapping) annotation);
        } else if (annotation.annotationType().isAssignableFrom(PatchMapping.class)) {
            return of((PatchMapping) annotation);
        } else if (annotation.annotationType().isAssignableFrom(DeleteMapping.class)) {
            return of((DeleteMapping) annotation);
        } else if (annotation.annotationType().isAssignableFrom(RequestMapping.class)) {
            return of((RequestMapping) annotation);
        }

        // GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
        throw new IllegalArgumentException("No support type of @RequestMapping.");
    }

    private static AnnotationMethodAttribute of(GetMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), HttpMethod.GET);
    }

    private static AnnotationMethodAttribute of(PostMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), HttpMethod.POST);
    }

    private static AnnotationMethodAttribute of(PutMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), HttpMethod.PUT);
    }

    private static AnnotationMethodAttribute of(PatchMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), HttpMethod.PATCH);
    }

    private static AnnotationMethodAttribute of(DeleteMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), HttpMethod.DELETE);
    }

    private static AnnotationMethodAttribute of(RequestMapping t) {
        RequestMethod[] m = t.method();
        Assert.isTrue(m.length == 1, "@RequestMapping method is wrong (only one parameter).");
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), HttpMethod.resolve(m[0].name()));
    }

    private static AnnotationMethodAttribute of(String[] path, String[] headers, String[] params, HttpMethod method) {
        return new AnnotationMethodAttribute(path, headers, params, method);
    }

    private static String[] getOrDefault(@NonNull String[] arg, String[] def) {
        Assert.isTrue(!(arg.length == 0 && def.length == 0), "path or value cannot be all empty.");
        return arg.length == 0 ? def : arg;
    }

    private static Annotation getMethodAnnotation(Method method) {
        Annotation[] annotations = method.getAnnotations();
        if (annotations == null || annotations.length != 1) {
            throw new IllegalArgumentException("@RequestMapping required and only one.");
        }

        return annotations[0];
    }
}
