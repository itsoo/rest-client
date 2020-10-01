package com.cupshe.restclient;

import com.cupshe.army.knife.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * ResponseProcessor
 *
 * @author zxy
 */
class ResponseProcessor {

    static Object convertToObject(String res, Method method) throws JsonProcessingException {
        if (isInconvertibleType(method.getReturnType()) || !isJsonFormatString(res)) {
            return res;
        }

        return convertToObject(res, method.getGenericReturnType());
    }

    private static Object convertToObject(String res, Type genericType) throws JsonProcessingException {
        if (genericType.getClass().isAssignableFrom(List.class)) {
            return JsonUtils.convertList(res, genericType.getClass());
        }

        JavaType targetJavaType = JsonUtils.getJavaType(genericType);
        return JsonUtils.jsonToObject(res, targetJavaType);
    }

    private static boolean isInconvertibleType(Class<?> returnType) {
        return returnType.isPrimitive() || returnType.isAssignableFrom(String.class);
    }

    private static boolean isJsonFormatString(String json) {
        return json.charAt(0) == '{' || json.charAt(0) == '[';
    }
}
