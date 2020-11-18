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

    String path;
    HttpMethod method;
    String[] headers;
    String[] params;

    private AnnotationMethodAttribute(String[] path, String[] headers, String[] params, HttpMethod method) {
        Assert.isTrue(path.length <= 1, "@RequestMapping value is wrong (only one parameter).");
        this.path = path.length == 1 ? path[0] : "";
        this.headers = headers;
        this.params = params;
        this.method = method;
    }

    static AnnotationMethodAttribute of(Method method) {
        return of(getMethodAnnotation(method));
    }

    boolean isPassingParamsOfUrl() {
        return GET.equals(method) || DELETE.equals(method);
    }

    boolean isPassingParamsOfForm() {
        return POST.equals(method) || PUT.equals(method) || PATCH.equals(method);
    }

    private static AnnotationMethodAttribute of(Annotation annotation) {
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
        RequestMethod[] m = t.method();
        Assert.isTrue(m.length == 1, "@RequestMapping property 'method' can only one.");
        return of(getOrDefault(t.path(), t.value()), t.headers(), t.params(), resolve(m[0].name()));
    }

    private static AnnotationMethodAttribute of(String[] path, String[] headers, String[] params, HttpMethod method) {
        return new AnnotationMethodAttribute(path, headers, params, method);
    }

    private static String[] getOrDefault(@NonNull String[] arg, @NonNull String[] def) {
        return arg.length == 0 ? def : arg;
    }

    private static Annotation getMethodAnnotation(Method method) {
        Annotation[] annotations = method.getDeclaredAnnotations();
        int count = 0;
        for (Annotation annotation : annotations) {
            try {
                of(annotation);
                count++;
            } catch (NoSupportMethodException ignore) {}
        }

        Assert.isTrue(count == 1, "@RequestMapping is required and only one.");
        return annotations[0];
    }
}
