package com.cupshe.restclient;

import com.cupshe.ak.json.JsonUtils;
import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.restclient.exception.ClassConvertException;
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
        if (isNotInconvertibleType(method.getReturnType())) {
            return res;
        }

        try {
            return convertToObject(res, method.getGenericReturnType());
        } catch (JsonProcessingException e) {
            throw new ClassConvertException(res, e);
        }
    }

    private static Object convertToObject(String res, Type genericType) throws JsonProcessingException {
        if (List.class.isAssignableFrom(genericType.getClass())) {
            return JsonUtils.convertList(res, genericType.getClass());
        }

        JavaType targetJavaType = JsonUtils.getJavaType(genericType);
        return JsonUtils.jsonToObject(res, targetJavaType);
    }

    private static boolean isNotInconvertibleType(Class<?> returnType) {
        return !ObjectClasses.isInconvertibleClass(returnType);
    }
}
