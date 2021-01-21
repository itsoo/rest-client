package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.core.Kvs;
import com.cupshe.ak.net.UriUtils;
import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.lang.PureFunction;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * RequestProcessor
 *
 * @author zxy
 */
@PureFunction
class RequestProcessor {

    static final String EXPRESSION_DELIMITER_PREFIX = "{";

    static final String EXPRESSION_DELIMITER_SUFFIX = "}";

    static final String ROOT_PROPERTY = StringUtils.EMPTY;

    private static final String EMPTY = StringUtils.EMPTY;

    static String processRequestParams(String url, Kvs args) {
        String rel = StringUtils.trimTrailingCharacter(url, '&');
        if (Objects.isNull(rel) || args.isEmpty()) {
            return rel;
        }

        StringJoiner params = new StringJoiner("&");
        for (Kv kv : args) {
            if (StringUtils.isNotBlank(kv.getKey())) {
                params.add(convertObjectToParams(kv.getKey(), kv.getValue()));
            }
        }

        return rel + getParamSeparator(rel) + params.toString();
    }

    static String processPathVariables(String url, Kvs args) {
        if (Objects.isNull(url) || args.isEmpty()) {
            return url;
        }

        StringBuilder result = new StringBuilder();
        Map<String, String> map = convertKvsToMap(args);
        int i = 0, j = i;
        while ((i = url.indexOf(EXPRESSION_DELIMITER_PREFIX, i)) != -1) {
            result.append(url, j, i); // no expression template delimiter
            j = url.indexOf(EXPRESSION_DELIMITER_SUFFIX, i);
            String key = url.substring(i, j);
            String value = map.get(key.substring(1).trim());
            if (Objects.nonNull(value)) {
                result.append(value);
            }

            i = ++j;
        }

        return result.append(url.substring(j)).toString();
    }

    static String processStandardUri(String prefix, String path) {
        String result = "/" + prefix + '/' + path;
        for (String repeatSp = "//"; result.contains(repeatSp); ) {
            result = StringUtils.replace(result, repeatSp, "/");
        }

        result = "/".equals(result) ? EMPTY : result;
        return result.endsWith("/")
                ? result.substring(0, result.length() - 1)
                : result;
    }

    static Object getRequestBodyOf(@NonNull Parameter[] params, @NonNull Object[] args) {
        for (int i = 0; i < params.length; i++) {
            RequestBody ann = AnnotationUtils.findAnnotation(params[i], RequestBody.class);
            if (Objects.nonNull(ann)) {
                return args[i];
            }
        }

        return null;
    }

    static Kvs getPathVariablesOf(@NonNull Parameter[] params, @NonNull Object[] args) {
        Kvs result = new Kvs();
        for (int i = 0; i < params.length; i++) {
            PathVariable ann = AnnotationUtils.findAnnotation(params[i], PathVariable.class);
            if (Objects.nonNull(ann)) {
                result.add(new Kv(ann.name(), args[i]));
            }
        }

        return result;
    }

    static Kvs getRequestParamsOf(@NonNull Parameter[] params, @NonNull Object[] args) {
        Kvs result = new Kvs();
        for (int i = 0; i < params.length; i++) {
            result.addAll(getSampleKvs(getPropertyName(params[i]), args[i]));
        }

        return result;
    }

    static Kvs getRequestParamsOf(@NonNull String[] params) {
        return convertStringToKvs(params, "=");
    }

    static Kvs getRequestHeadersOf(@NonNull Parameter[] params, @NonNull Object[] args) {
        Kvs result = new Kvs();
        for (int i = 0; i < params.length; i++) {
            RequestHeader ann = AnnotationUtils.findAnnotation(params[i], RequestHeader.class);
            if (Objects.nonNull(ann)) {
                result.add(new Kv(ann.name(), StringUtils.getOrEmpty(args[i])));
            }
        }

        return result;
    }

    static Kvs getRequestHeadersOf(@NonNull String[] params) {
        return convertStringToKvs(params, ":");
    }

    private static String convertObjectToParams(String property, Object arg) {
        StringJoiner joiner = new StringJoiner("&");
        for (Kv kv : getSampleKvs(property, arg)) {
            joiner.add(kv.getKey() + '=' + UriUtils.encode(kv.getValue()));
        }

        return joiner.toString();
    }

    private static Kvs convertStringToKvs(@NonNull String[] params, String sp) {
        Kvs result = new Kvs();
        for (String param : params) {
            String[] kv = StringUtils.split(param, sp);
            if (Objects.nonNull(kv)) {
                result.add(new Kv(kv[0], StringUtils.trimToEmpty(kv[1])));
            }
        }

        return result;
    }

    private static Map<String, String> convertKvsToMap(@NonNull Kvs kvs) {
        // maybe contains repeat keys
        Map<String, String> result = new HashMap<>(kvs.size() << 1);
        for (Kv kv : kvs) {
            result.put(kv.getKey(), StringUtils.getOrEmpty(kv.getValue()));
        }

        return Collections.unmodifiableMap(result);
    }

    private static Kvs getSampleKvs(String property, Object arg) {
        if (Objects.isNull(arg)) {
            return Kvs.emptyKvs();
        }

        Kvs result = new Kvs();
        if (!ObjectClasses.isInconvertibleClass(arg.getClass()) && StringUtils.isNotBlank(property)) {
            // is literals
            result.add(new Kv(property, arg));
        } else if (arg instanceof Kv) {
            Kv kv = (Kv) arg;
            result.addAll(getSampleKvs(getObjectKey(property, kv.getKey()), kv.getValue()));
        } else if (arg instanceof Map) {
            for (Map.Entry<?, ?> me : ((Map<?, ?>) arg).entrySet()) {
                result.addAll(getSampleKvs(getCollectionKey(property, me.getKey()), me.getValue()));
            }
        } else if (arg instanceof List) {
            for (int i = 0, size = ((List<?>) arg).size(); i < size; i++) {
                result.addAll(getSampleKvs(getCollectionKey(property, i), ((List<?>) arg).get(i)));
            }
        } else if (arg.getClass().isArray()) {
            for (int i = 0, length = Array.getLength(arg); i < length; i++) {
                result.addAll(getSampleKvs(getArrayKey(property), Array.get(arg, i)));
            }
        } else {
            for (Kv kv : ObjectClasses.getObjectProperties(arg)) {
                result.addAll(getSampleKvs(getObjectKey(property, kv.getKey()), kv.getValue()));
            }
        }

        return result;
    }

    private static String getPropertyName(Parameter param) {
        RequestParam ann = AnnotationUtils.findAnnotation(param, RequestParam.class);
        return Objects.isNull(ann) ? ROOT_PROPERTY : ann.name();
    }

    private static char getParamSeparator(String uri) {
        return uri.lastIndexOf('?') != -1 ? '&' : '?';
    }

    private static String getObjectKey(String prefix, String key) {
        return StringUtils.isBlank(prefix) ? key : (prefix + '.' + key);
    }

    private static String getCollectionKey(String prefix, Object key) {
        return StringUtils.defaultIfBlank(prefix, EMPTY) + '[' + key + ']';
    }

    private static String getArrayKey(String prefix) {
        return StringUtils.defaultIfBlank(prefix, EMPTY) + "[]";
    }
}
