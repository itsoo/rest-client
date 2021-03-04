package com.cupshe.restclient;

import com.cupshe.restclient.exception.NoSupportMethodException;
import com.cupshe.restclient.lang.HttpsSupported;
import com.cupshe.restclient.lang.PureFunction;
import com.cupshe.restclient.parser.ExpressionParser;
import com.cupshe.restclient.parser.PropertyValueExpressionParser;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

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

    final boolean httpsSupported;

    private AnnotationMethodAttribute(
            String[] paths, String[] headers, String[] params, HttpMethod method, boolean httpsSupported) {

        this.path = paths.length == 1 ? paths[0] : "";
        this.paths = paths;
        this.headers = headers;
        this.params = params;
        this.method = method;
        this.httpsSupported = httpsSupported;
    }

    @PureFunction
    boolean isPassingParamsOfUrl() {
        return GET.equals(method) || DELETE.equals(method);
    }

    @PureFunction
    boolean isPassingParamsOfForm() {
        return POST.equals(method) || PUT.equals(method) || PATCH.equals(method);
    }

    AnnotationMethodAttribute process(Environment env) {
        ExpressionParser<?> parser = new PropertyValueExpressionParser(env);
        for (int i = 0; i < headers.length; i++) {
            headers[i] = parser.process(headers[i]);
        }

        for (int i = 0; i < params.length; i++) {
            params[i] = parser.process(params[i]);
        }

        return this;
    }

    @PureFunction
    static AnnotationMethodAttribute of(Method method) {
        Annotation ann;
        boolean httpsSupported = supportHttpsProtocol(method);
        if (Objects.nonNull(ann = findAnnotation(method, GetMapping.class))) {
            return of(ann, httpsSupported);
        } else if (Objects.nonNull(ann = findAnnotation(method, PostMapping.class))) {
            return of(ann, httpsSupported);
        } else if (Objects.nonNull(ann = findAnnotation(method, PutMapping.class))) {
            return of(ann, httpsSupported);
        } else if (Objects.nonNull(ann = findAnnotation(method, PatchMapping.class))) {
            return of(ann, httpsSupported);
        } else if (Objects.nonNull(ann = findAnnotation(method, DeleteMapping.class))) {
            return of(ann, httpsSupported);
        } else if (Objects.nonNull(ann = findAnnotation(method, RequestMapping.class))) {
            return of(ann, httpsSupported);
        }

        throw new NoSupportMethodException();
    }

    @PureFunction
    static AnnotationMethodAttribute of(Annotation ann, boolean httpsSupported) {
        if (GetMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((GetMapping) ann, httpsSupported);
        } else if (PostMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((PostMapping) ann, httpsSupported);
        } else if (PutMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((PutMapping) ann, httpsSupported);
        } else if (PatchMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((PatchMapping) ann, httpsSupported);
        } else if (DeleteMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((DeleteMapping) ann, httpsSupported);
        } else if (RequestMapping.class.isAssignableFrom(ann.annotationType())) {
            return of((RequestMapping) ann, httpsSupported);
        }

        throw new NoSupportMethodException();
    }

    private static AnnotationMethodAttribute of(GetMapping t, boolean httpsSupported) {
        return of(t.path(), t.headers(), t.params(), GET, httpsSupported);
    }

    private static AnnotationMethodAttribute of(PostMapping t, boolean httpsSupported) {
        return of(t.path(), t.headers(), t.params(), POST, httpsSupported);
    }

    private static AnnotationMethodAttribute of(PutMapping t, boolean httpsSupported) {
        return of(t.path(), t.headers(), t.params(), PUT, httpsSupported);
    }

    private static AnnotationMethodAttribute of(PatchMapping t, boolean httpsSupported) {
        return of(t.path(), t.headers(), t.params(), PATCH, httpsSupported);
    }

    private static AnnotationMethodAttribute of(DeleteMapping t, boolean httpsSupported) {
        return of(t.path(), t.headers(), t.params(), DELETE, httpsSupported);
    }

    private static AnnotationMethodAttribute of(RequestMapping t, boolean httpsSupported) {
        return of(t.path(), t.headers(), t.params(), resolve(t.method()[0].name()), httpsSupported);
    }

    private static AnnotationMethodAttribute of(
            String[] paths, String[] headers, String[] params, HttpMethod method, boolean httpsSupported) {

        return new AnnotationMethodAttribute(paths, headers, params, method, httpsSupported);
    }

    private static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(method, annotationType);
    }

    private static boolean supportHttpsProtocol(Method method) {
        HttpsSupported ann = AnnotationUtils.findAnnotation(method, HttpsSupported.class);
        return Objects.nonNull(ann) || supportHttpsProtocol(method.getDeclaringClass());
    }

    private static boolean supportHttpsProtocol(Class<?> clazz) {
        HttpsSupported ann = AnnotationUtils.findAnnotation(clazz, HttpsSupported.class);
        return Objects.nonNull(ann);
    }
}
