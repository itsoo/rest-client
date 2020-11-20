package com.cupshe.restclient;

import com.cupshe.restclient.exception.NoSupportMethodException;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
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

    String[] paths;
    String path;
    HttpMethod method;
    String[] headers;
    String[] params;

    private AnnotationMethodAttribute(String[] path, String[] headers, String[] params, HttpMethod method) {
        this.paths = path;
        this.path = paths.length == 1 ? paths[0] : "";
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
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), GET);
    }

    private static AnnotationMethodAttribute of(PostMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), POST);
    }

    private static AnnotationMethodAttribute of(PutMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), PUT);
    }

    private static AnnotationMethodAttribute of(PatchMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), PATCH);
    }

    private static AnnotationMethodAttribute of(DeleteMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), DELETE);
    }

    private static AnnotationMethodAttribute of(RequestMapping t) {
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), resolve(t.method()[0].name()));
    }

    private static AnnotationMethodAttribute of(String[] path, String[] headers, String[] params, HttpMethod method) {
        return new AnnotationMethodAttribute(path, headers, params, method);
    }

    private static String[] getOrDefault(@NonNull String[] arg, @NonNull String[] def) {
        return arg.length == 0 ? def : arg;
    }
}
