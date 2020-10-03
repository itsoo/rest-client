package com.cupshe.restclient;

import com.cupshe.army.knife.BeanUtils;
import com.cupshe.army.knife.Kv;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RequestProcessor
 *
 * @author zxy
 */
class RequestProcessor {

    private static final Pattern PATTERN = Pattern.compile("(\\{[^}]*})");

    static String processRequestParamOf(String url, List<Kv> args) {
        if (url == null || args == null) {
            return null;
        }

        if (args.isEmpty()) {
            return url;
        }

        char sp = getQuerySeparator(url);
        StringBuilder result = new StringBuilder(url);
        for (int i = 0, size = args.size(); i < size; i++) {
            if (i > 0) {
                sp = '&';
            }

            String key = args.get(i).getKey();
            if (StringUtils.hasText(key)) {
                result.append(sp).append(key).append('=').append(encode(args.get(i).getValue()));
            }
        }

        return result.toString();
    }

    static String processPathVariableOf(String url, List<Kv> args) {
        if (url == null || args == null) {
            return null;
        }

        String result = url;
        Iterator<Kv> it = args.iterator();
        for (Kv kv; it.hasNext(); ) {
            kv = it.next();
            String key = kv.getKey();
            Object value = kv.getValue();
            if (StringUtils.hasText(key) && value != null) {
                result = StringUtils.replace(result, '{' + key + '}', value.toString());
                it.remove();
            }
        }

        Matcher m = PATTERN.matcher(result);
        if (m.find()) {
            int size = Math.min(m.groupCount(), args.size());
            for (int i = 0; i < size; i++) {
                Object value = args.get(i).getValue();
                if (value != null) {
                    result = StringUtils.replace(result, m.group(i + 1), value.toString());
                }
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
        return getQuerySeparator(uri) + String.join("&", params);
    }

    static Object processRequestBodyOf(Parameter[] params, Object[] args) {
        for (int i = 0; i < params.length; i++) {
            RequestBody da = params[i].getDeclaredAnnotation(RequestBody.class);
            if (da != null) {
                return args[i];
            }
        }

        return null;
    }

    static List<Kv> processPathVariablesOf(Parameter[] params, Object[] args) {
        List<Kv> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            PathVariable da = params[i].getDeclaredAnnotation(PathVariable.class);
            if (da != null) {
                result.add(new Kv(da.value(), args[i]));
            }
        }

        return result;
    }

    static List<Kv> processRequestParamsOf(Parameter[] params, Object[] args) {
        List<Kv> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            RequestParam da = params[i].getDeclaredAnnotation(RequestParam.class);
            if (da != null) {
                result.add(new Kv(da.value(), args[i]));
            }

            if (isEmptyAnnotations(params[i].getDeclaredAnnotations())) {
                result.addAll(BeanUtils.getObjectProperties(args[i]));
            }
        }

        return result;
    }

    private static char getQuerySeparator(String uri) {
        return uri.lastIndexOf('?') > -1 ? '&' : '?';
    }

    @SneakyThrows
    private static String encode(Object value) {
        return StringUtils.isEmpty(value) ? "" : UriUtils.encode(value.toString(), StandardCharsets.UTF_8);
    }

    @SafeVarargs
    private static <T> boolean isEmptyAnnotations(T... annotations) {
        return annotations == null || annotations.length == 0;
    }
}
