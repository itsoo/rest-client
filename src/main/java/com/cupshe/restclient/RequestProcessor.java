package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.net.UriUtils;
import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.ak.text.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RequestProcessor
 *
 * @author zxy
 */
class RequestProcessor {

    static final String ROOT_PROPERTY = StringUtils.EMPTY;

    private static final String EMPTY = StringUtils.EMPTY;

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("(\\{[^}]*})");

    static String processRequestParamOf(String url, List<Kv> args) {
        String rel = StringUtils.trimTrailingCharacter(url, '&');
        if (rel == null || CollectionUtils.isEmpty(args)) {
            return rel;
        }

        StringJoiner q = new StringJoiner("&");
        for (Kv kv : args) {
            if (StringUtils.isNotBlank(kv.getKey())) {
                q.add(convertObjectToQueryUri(kv.getKey(), kv.getValue()));
            }
        }

        return rel + getQuerySeparator(rel) + q.toString();
    }

    static String processPathVariableOf(String url, List<Kv> args) {
        if (url == null || CollectionUtils.isEmpty(args)) {
            return url;
        }

        String result = url;
        Map<String, String> map = convertKvsToMap(args);
        Matcher m = PATH_VARIABLE_PATTERN.matcher(result);
        while (m.find()) {
            String key = m.group(1);
            String value = map.get(key.substring(1, key.length() - 1).trim());
            if (value != null) {
                result = StringUtils.replace(result, key, value);
            }
        }

        return result;
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
            RequestBody annotation = AnnotationUtils.findAnnotation(params[i], RequestBody.class);
            if (annotation != null) {
                return args[i];
            }
        }

        return null;
    }

    static List<Kv> getPathVariablesOf(@NonNull Parameter[] params, @NonNull Object[] args) {
        List<Kv> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            PathVariable annotation = AnnotationUtils.findAnnotation(params[i], PathVariable.class);
            if (annotation != null) {
                result.add(new Kv(annotation.name(), args[i]));
            }
        }

        return result;
    }

    static List<Kv> getRequestParamsOf(@NonNull Parameter[] params, @NonNull Object[] args) {
        List<Kv> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            result.addAll(getSampleKvs(getPropertyName(params[i]), args[i]));
        }

        return result;
    }

    static List<Kv> getRequestParamsOf(@NonNull String[] params, boolean needEncode) {
        List<Kv> result = convertStringToKvs(params);
        if (!needEncode) {
            return result;
        }

        return result
                .stream()
                .map(t -> new Kv(t.getKey(), UriUtils.encode(t.getValue())))
                .collect(Collectors.toList());
    }

    static List<Kv> getRequestParamsOf(@NonNull String[] params) {
        return getRequestParamsOf(params, true);
    }

    static List<Kv> getRequestParamsOf(String property, Object arg) {
        return getSampleKvs(property, arg);
    }

    static List<Kv> getRequestHeadersOf(@NonNull Parameter[] params, @NonNull Object[] args) {
        List<Kv> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            RequestHeader annotation = AnnotationUtils.findAnnotation(params[i], RequestHeader.class);
            if (annotation != null) {
                result.add(new Kv(annotation.name(), StringUtils.getOrEmpty(args[i])));
            }
        }

        return result;
    }

    static List<Kv> getRequestHeadersOf(@NonNull String[] params) {
        return convertStringToKvs(params);
    }

    private static String convertObjectToQueryUri(String property, Object arg) {
        StringJoiner joiner = new StringJoiner("&");
        getSampleKvs(property, arg)
                .stream()
                .map(t -> t.getKey() + '=' + UriUtils.encode(t.getValue()))
                .forEach(joiner::add);
        return joiner.toString();
    }

    private static List<Kv> convertStringToKvs(@NonNull String[] params) {
        List<Kv> result = new ArrayList<>(params.length);
        for (String param : params) {
            String[] kv = StringUtils.split(param, "=");
            if (kv != null) {
                result.add(new Kv(kv[0], kv[1]));
            }
        }

        return result;
    }

    private static Map<String, String> convertKvsToMap(@NonNull List<Kv> args) {
        Map<String, String> result = new HashMap<>(args.size() << 1);
        Map<String, Object> map = args.stream().collect(Collectors.toMap(Kv::getKey, Kv::getValue));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), StringUtils.getOrEmpty(entry.getValue()));
        }

        return result;
    }

    private static List<Kv> getSampleKvs(String property, Object arg) {
        if (arg == null) {
            return Collections.emptyList();
        }

        List<Kv> result = new ArrayList<>();
        if (!ObjectClasses.isInconvertibleClass(arg.getClass())) {
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
        RequestParam annotation = AnnotationUtils.findAnnotation(param, RequestParam.class);
        return annotation == null ? ROOT_PROPERTY : annotation.name();
    }

    private static char getQuerySeparator(String uri) {
        return uri.lastIndexOf('?') != -1 ? '&' : '?';
    }

    private static String getObjectKey(String prefix, String key) {
        return StringUtils.isBlank(prefix) ? key : prefix + '.' + key;
    }

    private static String getCollectionKey(String prefix, Object key) {
        return StringUtils.defaultIfBlank(prefix, EMPTY) + '[' + key + ']';
    }

    private static String getArrayKey(String prefix) {
        return StringUtils.defaultIfBlank(prefix, EMPTY) + "[]";
    }
}
