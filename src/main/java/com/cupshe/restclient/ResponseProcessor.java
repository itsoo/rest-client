package com.cupshe.restclient;

import com.cupshe.ak.json.JsonUtils;
import com.cupshe.restclient.exception.ClassConvertException;
import com.cupshe.restclient.util.ObjectClassUtils;
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

    static Object convertToObject(String res, Method method) {
        if (isNotInconvertibleType(method.getReturnType()) || isNotJsonFormatString(res)) {
            return res;
        }

        try {
            return convertToObject(res, method.getGenericReturnType());
        } catch (JsonProcessingException e) {
            throw new ClassConvertException(res, e);
        }
    }

    private static Object convertToObject(String res, Type genericType) throws JsonProcessingException {
        if (genericType.getClass().isAssignableFrom(List.class)) {
            return JsonUtils.convertList(res, genericType.getClass());
        }

        JavaType targetJavaType = JsonUtils.getJavaType(genericType);
        return JsonUtils.jsonToObject(res, targetJavaType);
    }

    private static boolean isNotInconvertibleType(Class<?> returnType) {
        return !ObjectClassUtils.isInconvertibleClass(returnType);
    }

    private static boolean isNotJsonFormatString(String json) {
        return json.charAt(0) != '{' && json.charAt(0) != '[';
    }
}
