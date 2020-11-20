package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.util.ObjectClassUtils;
import com.cupshe.ak.net.UriUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
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

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("(\\{[^}]*})");

    private static final String EMPTY = StringUtils.EMPTY;

    private static final String ROOT_PROPERTY = EMPTY;

    static String processRequestParamOf(String url, List<Kv> args) {
        String rel = StringUtils.trimTrailingCharacter(url, '&');
        if (rel == null || CollectionUtils.isEmpty(args)) {
            return rel;
        }

        StringJoiner q = new StringJoiner("&");
        for (Kv kv : args) {
            if (StringUtils.isNotBlank(kv.getKey())) {
                q.add(convertObjectToQueryUrl(kv.getKey(), kv.getValue()));
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
                result.addAll(convertToSampleKvs(da.value(), args[i]));
            }

            if (isEmptyAnnotations(params[i].getDeclaredAnnotations())) {
                result.addAll(convertToSampleKvs(params[i].getName(), args[i]));
            }
        }

        return result;
    }

    static MultiValueMap<String, Object> convertObjectsToMultiValueMap(Parameter[] params, Object[] args) {
        MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
        for (int i = 0; i < params.length; i++) {
//            result.addAll(convertObjectToMultiValueMap(ROOT_PROPERTY, args[i]));
            result.addAll(convertObjectToMultiValueMap(params[i].getName(), args[i]));
        }

        return result;
    }

    static MultiValueMap<String, Object> convertObjectToMultiValueMap(String property, Object arg) {
        MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
        for (Kv kv : convertToSampleKvs(property, arg)) {
            result.add(kv.getKey(), kv.getValue());
        }

        return result;
    }

    static String convertObjectToQueryUrl(String property, Object arg) {
        StringJoiner joiner = new StringJoiner("&");
        convertToSampleKvs(property, arg)
                .stream()
                .map(t -> t.getKey() + '=' + UriUtils.encode(t.getValue()))
                .forEach(joiner::add);
        return joiner.toString();
    }

    private static List<Kv> convertToSampleKvs(String property, Object arg) {
        if (arg == null) {
            return Collections.emptyList();
        }

        List<Kv> result = new ArrayList<>();
        if (!ObjectClassUtils.isInconvertibleClass(arg.getClass())) {
            result.add(new Kv(property, arg));
            return result;
        }

        if (Kv.class.isAssignableFrom(arg.getClass())) {
            result.addAll(convertToSampleKvs(getObjectKey(property, ((Kv) arg).getKey()), ((Kv) arg).getValue()));
        } else if (Map.class.isAssignableFrom(arg.getClass())) {
            for (Map.Entry<?, ?> me : ((Map<?, ?>) arg).entrySet()) {
                result.addAll(convertToSampleKvs(getCollectionKey(property, me.getKey()), me.getValue()));
            }
        } else if (List.class.isAssignableFrom(arg.getClass())) {
            for (int i = 0, size = ((List<?>) arg).size(); i < size; i++) {
                result.addAll(convertToSampleKvs(getCollectionKey(property, i), ((List<?>) arg).get(i)));
            }
        } else if (arg.getClass().isArray()) {
            for (int i = 0, length = Array.getLength(arg); i < length; i++) {
                result.addAll(convertToSampleKvs(getArrayKey(property), Array.get(arg, i)));
            }
        } else {
            for (Kv kv : ObjectClassUtils.getObjectProperties(arg)) {
                result.addAll(convertToSampleKvs(getObjectKey(property, kv.getKey()), kv.getValue()));
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

    private static String getObjectKey(String prefix, String key) {
        return StringUtils.isBlank(prefix) ? key : prefix + '.' + key;
    }

    private static String getCollectionKey(String prefix, Object key) {
        return StringUtils.isBlank(prefix) ? "[" + key + ']' : prefix + '[' + key + ']';
    }

    private static String getArrayKey(String prefix) {
        return StringUtils.isBlank(prefix) ? "[]" : prefix + "[]";
    }

    private static char getQuerySeparator(String uri) {
        return uri.lastIndexOf('?') > -1 ? '&' : '?';
    }

    @SafeVarargs
    private static <T> boolean isEmptyAnnotations(T... annotations) {
        return annotations == null || annotations.length == 0;
    }
}
