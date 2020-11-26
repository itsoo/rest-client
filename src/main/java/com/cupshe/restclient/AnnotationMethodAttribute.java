package com.cupshe.restclient;

import com.cupshe.restclient.exception.NoSupportMethodException;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.springframework.http.HttpMethod.*;

/**
 * AnnotationMethodAttribute
 *
 * @author zxy
 */
class AnnotationMethodAttribute {

    String path;
    String[] paths;
    String[] headers;
    String[] params;
    HttpMethod method;

    private AnnotationMethodAttribute(String[] paths, String[] headers, String[] params, HttpMethod method) {
        this.path = paths.length == 1 ? paths[0] : "";
        this.paths = paths;
        this.headers = headers;
        this.params = params;
        this.method = method;
    }

    static AnnotationMethodAttribute of(Method method) {
        return of(method.getDeclaredAnnotations()[0]);
    }

    boolean isPassingParamsOfUrl() {
        return GET.equals(method) || DELETE.equals(method);
    }

    boolean isPassingParamsOfForm() {
        return POST.equals(method) || PUT.equals(method) || PATCH.equals(method);
    }

    static AnnotationMethodAttribute of(Annotation annotation) {
        if (GetMapping.class.isAssignableFrom(annotation.annotationType())) {
            return of((GetMapping) annotation);
        } else if (PostMapping.class.isAssignableFrom(annotation.annotationType())) {
            return of((PostMapping) annotation);
        } else if (PutMapping.class.isAssignableFrom(annotation.annotationType())) {
            return of((PutMapping) annotation);
        } else if (PatchMapping.class.isAssignableFrom(annotation.annotationType())) {
            return of((PatchMapping) annotation);
        } else if (DeleteMapping.class.isAssignableFrom(annotation.annotationType())) {
            return of((DeleteMapping) annotation);
        } else if (RequestMapping.class.isAssignableFrom(annotation.annotationType())) {
            return of((RequestMapping) annotation);
        } else {
            throw new NoSupportMethodException();
        }
    }

    private static AnnotationMethodAttribute of(GetMapping t) {
        return of(t.path(), t.headers(), t.params(), GET);
    }

    private static AnnotationMethodAttribute of(PostMapping t) {
        return of(t.path(), t.headers(), t.params(), POST);
    }

    private static AnnotationMethodAttribute of(PutMapping t) {
        return of(t.path(), t.headers(), t.params(), PUT);
    }

    private static AnnotationMethodAttribute of(PatchMapping t) {
        return of(t.path(), t.headers(), t.params(), PATCH);
    }

    private static AnnotationMethodAttribute of(DeleteMapping t) {
        return of(t.path(), t.headers(), t.params(), DELETE);
    }

    private static AnnotationMethodAttribute of(RequestMapping t) {
        return of(t.path(), t.headers(), t.params(), resolve(t.method()[0].name()));
    }

    private static AnnotationMethodAttribute of(String[] paths, String[] headers, String[] params, HttpMethod method) {
        return new AnnotationMethodAttribute(paths, headers, params, method);
    }
}
