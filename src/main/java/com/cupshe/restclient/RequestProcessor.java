package com.cupshe.restclient;

import com.cupshe.army.knife.BeanUtils;
import com.cupshe.army.knife.Kv;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLEncoder;
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

        char sp = url.lastIndexOf('?') > -1 ? '&' : '?';
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
                result = result.replace('{' + key + '}', value.toString());
                it.remove();
            }
        }

        Matcher m = PATTERN.matcher(result);
        if (m.find()) {
            int size = Math.min(m.groupCount(), args.size());
            for (int i = 0; i < size; i++) {
                Object value = args.get(i).getValue();
                if (value != null) {
                    result = result.replace(m.group(i + 1), value.toString());
                }
            }
        }

        return result;
    }

    static String processStandardUri(String prefix, String path) {
        return prefix.endsWith("/") || path.startsWith("/") ? prefix + path : prefix + '/' + path;
    }

    static Object processRequestBodyOf(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            RequestBody anno = params[i].getDeclaredAnnotation(RequestBody.class);
            if (anno != null) {
                return args[i];
            }
        }

        return null;
    }

    static List<Kv> processPathVariablesOf(Method method, Object[] args) {
        List<Kv> result = new ArrayList<>();
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            PathVariable anno = params[i].getDeclaredAnnotation(PathVariable.class);
            if (anno != null) {
                result.add(new Kv(anno.value(), args[i]));
            }
        }

        return result;
    }

    static List<Kv> processRequestParamsOf(Method method, Object[] args) {
        List<Kv> result = new ArrayList<>();
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            RequestParam anno = params[i].getDeclaredAnnotation(RequestParam.class);
            if (anno != null) {
                result.add(new Kv(anno.value(), args[i]));
            }

            if (isEmptyAnnotations(params[i].getDeclaredAnnotations())) {
                result.addAll(BeanUtils.getObjectProperties(args[i]));
            }
        }

        return result;
    }

    @SneakyThrows
    private static String encode(Object value) {
        return StringUtils.isEmpty(value) ? "" : URLEncoder.encode(value.toString(), "UTF-8");
    }

    @SafeVarargs
    private static <T> boolean isEmptyAnnotations(T... annotations) {
        return annotations == null || annotations.length == 0;
    }
}
