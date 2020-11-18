package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.util.BeanUtils;
import com.cupshe.restclient.util.UriUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    private static final Pattern PATTERN = Pattern.compile("(\\{[^}]*})");

    static String processRequestParamOf(String url, List<Kv> args) {
        if (url == null) {
            return null;
        }

        if (CollectionUtils.isEmpty(args)) {
            return url;
        }

        char sp = getQuerySeparator(url);
        StringBuilder result = new StringBuilder(url);
        for (int i = 0, size = args.size(); i < size; i++) {
            if (i > 0) {
                sp = '&';
            }

            String key = args.get(i).getKey();
            if (StringUtils.isNotBlank(key)) {
                result.append(sp).append(convertObjectToQueryUrl(args.get(i).getValue()));
            }
        }

        return result.toString();
    }

    static String processPathVariableOf(String url, List<Kv> args) {
        if (url == null || args == null) {
            return url;
        }

        String result = url;
        Map<String, String> map = convertKvsToMap(args);
        Matcher m = PATTERN.matcher(result);
        while (m.find()) {
            String key = m.group(1);
            String value = map.get(key.substring(1, key.length() - 1).trim());
            if (value != null) {
                result = StringUtils.replace(result, key, value);
            }
        }

        Assert.isTrue(!result.contains("{"), "Wrong parameter defined by request path.");
        return result;
    }

    static String processStandardUri(String prefix, String path) {
        String result = '/' + prefix + '/' + path;
        for (String repeatSp = "//"; result.contains(repeatSp); ) {
            result = StringUtils.replace(result, repeatSp, "/");
        }

        result = "/".equals(result) ? "" : result;
        return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
    }

    static String processParamsOfUri(String uri, String[] params) {
        // query params
        String[] qps = params.clone();
        for (int i = 0; i < qps.length; i++) {
            String[] kv = StringUtils.split(qps[i], "=");
            if (kv != null) {
                qps[i] = kv[0] + '=' + UriUtils.encode(kv[1]);
            }
        }

        return uri + (qps.length == 0 ? "" : getQuerySeparator(uri) + String.join("&", qps));
    }

    static Object processRequestBodyOf(Parameter[] params, Object[] args) {
        for (int i = 0; i < params.length; i++) {
            RequestBody da = AnnotationUtils.findAnnotation(params[i], RequestBody.class);
            if (da != null) {
                return args[i];
            }
        }

        return null;
    }

    static List<Kv> processPathVariablesOf(Parameter[] params, Object[] args) {
        List<Kv> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            PathVariable da = AnnotationUtils.findAnnotation(params[i], PathVariable.class);
            if (da != null) {
                result.add(new Kv(da.value(), args[i]));
            }
        }

        return result;
    }

    static List<Kv> processRequestParamsOf(Parameter[] params, Object[] args) {
        List<Kv> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            RequestParam da = AnnotationUtils.findAnnotation(params[i], RequestParam.class);
            if (da != null) {
                result.add(new Kv(da.value(), args[i]));
            }

            if (isEmptyAnnotations(params[i].getDeclaredAnnotations())) {
                result.addAll(BeanUtils.getObjectProperties(args[i]));
            }
        }

        return result;
    }

    static MultiValueMap<String, Object> convertObjectsToMultiValueMap(Object... args) {
        MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
        for (Object arg : args) {
            result.addAll(convertObjectToMultiValueMap(arg));
        }

        return result;
    }

    static MultiValueMap<String, Object> convertObjectToMultiValueMap(Object arg) {
        MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
        for (Kv kv : convertKvToSampleKv("", arg)) {
            result.add(kv.getKey(), kv.getValue());
        }

        return result;
    }

    static String convertObjectToQueryUrl(Object arg) {
        StringJoiner joiner = new StringJoiner("&");
        convertKvToSampleKv("", arg)
                .stream()
                .map(t -> t.getKey() + '=' + UriUtils.encode(t.getValue()))
                .forEach(joiner::add);
        return joiner.toString();
    }

    private static List<Kv> convertKvToSampleKv(String property, Object arg) {
        if (arg == null) {
            return Collections.emptyList();
        }

        List<Kv> result = new ArrayList<>();
        if (!BeanUtils.isInconvertibleClass(arg.getClass())) {
            result.add(new Kv(property, arg));
            return result;
        }

        if (Kv.class.isAssignableFrom(arg.getClass())) { // Kv.class
            Kv kv = (Kv) arg;
            result.addAll(convertKvToSampleKv(getPropertyKey(property, kv.getKey()), kv.getValue()));
        } else if (Map.class.isAssignableFrom(arg.getClass())) { // Map.class
            Map<?, ?> map = (Map<?, ?>) arg;
            for (Map.Entry<?, ?> me : map.entrySet()) {
                result.addAll(convertKvToSampleKv(getContainerKey(property, me.getKey()), me.getValue()));
            }
        } else if (List.class.isAssignableFrom(arg.getClass())) { // List.class
            List<?> list = (List<?>) arg;
            for (int i = 0, size = list.size(); i < size; i++) {
                Object obj = list.get(i);
                result.addAll(convertKvToSampleKv(getContainerKey(property, i), obj));
            }
        } else if (arg.getClass().isArray()) { // array
            int length = Array.getLength(arg);
            for (int i = 0; i < length; i++) {
                Object obj = Array.get(arg, i);
                result.addAll(convertKvToSampleKv(getContainerKey(property, i), obj));
            }
        } else { // pojo
            List<Kv> kvs = BeanUtils.getObjectProperties(arg);
            for (Kv kv : kvs) {
                result.addAll(convertKvToSampleKv(getPropertyKey(property, kv.getKey()), kv.getValue()));
            }
        }

        return result;
    }

    private static Map<String, String> convertKvsToMap(@NonNull List<Kv> args) {
        Map<String, String> result = new HashMap<>(args.size() << 1);
        Map<String, Object> map = args.stream().collect(Collectors.toMap(Kv::getKey, Kv::getValue));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
        }

        return result;
    }

    private static String getPropertyKey(String prefix, String key) {
        return StringUtils.isBlank(prefix) ? key : prefix + '.' + key;
    }

    private static String getContainerKey(String prefix, Object key) {
        return StringUtils.isBlank(prefix) ? "[" + key + ']' : prefix + '[' + key + ']';
    }

    private static char getQuerySeparator(String uri) {
        return uri.lastIndexOf('?') > -1 ? '&' : '?';
    }

    @SafeVarargs
    private static <T> boolean isEmptyAnnotations(T... annotations) {
        return annotations == null || annotations.length == 0;
    }
}
