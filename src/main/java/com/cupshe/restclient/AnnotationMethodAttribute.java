package com.cupshe.restclient;

import com.cupshe.restclient.exception.NoSupportMethodException;
import com.cupshe.restclient.lang.PureFunction;
import org.springframework.core.annotation.AnnotationUtils;
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

    final String path;

    final String[] paths;

    final String[] headers;

    final String[] params;

    final HttpMethod method;

    private AnnotationMethodAttribute(String[] paths, String[] headers, String[] params, HttpMethod method) {
        this.path = paths.length == 1 ? paths[0] : "";
        this.paths = paths;
        this.headers = headers;
        this.params = params;
        this.method = method;
    }

    @PureFunction
    boolean isPassingParamsOfUrl() {
        return GET.equals(method) || DELETE.equals(method);
    }

    @PureFunction
    boolean isPassingParamsOfForm() {
        return POST.equals(method) || PUT.equals(method) || PATCH.equals(method);
    }

    @PureFunction
    static AnnotationMethodAttribute of(Method method) {
        Annotation ann;
        if ((ann = findAnnotation(method, GetMapping.class)) != null) {
            return of(ann);
        } else if ((ann = findAnnotation(method, PostMapping.class)) != null) {
            return of(ann);
        } else if ((ann = findAnnotation(method, PutMapping.class)) != null) {
            return of(ann);
        } else if ((ann = findAnnotation(method, PatchMapping.class)) != null) {
            return of(ann);
        } else if ((ann = findAnnotation(method, DeleteMapping.class)) != null) {
            return of(ann);
        } else if ((ann = findAnnotation(method, RequestMapping.class)) != null) {
            return of(ann);
        }

        throw new NoSupportMethodException();
    }

    @PureFunction
    static AnnotationMethodAttribute of(Annotation ann) {
        if (GetMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((GetMapping) ann);
        } else if (PostMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((PostMapping) ann);
        } else if (PutMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((PutMapping) ann);
        } else if (PatchMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((PatchMapping) ann);
        } else if (DeleteMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((DeleteMapping) ann);
        } else if (RequestMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((RequestMapping) ann);
        }

        throw new NoSupportMethodException();
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

    private static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(method, annotationType);
    }
}
