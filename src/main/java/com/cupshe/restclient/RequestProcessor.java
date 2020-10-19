package com.cupshe.restclient;

import com.cupshe.ak.BeanUtils;
import com.cupshe.ak.core.Kv;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * RequestProcessor
 *
 * @author zxy
 */
class RequestProcessor {

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

    @SafeVarargs
    private static <T> boolean isEmptyAnnotations(T... annotations) {
        return annotations == null || annotations.length == 0;
    }
}
