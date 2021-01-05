package com.cupshe.restclient;

import com.cupshe.ak.json.JsonUtils;
import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.restclient.exception.ClassConvertException;
import com.cupshe.restclient.lang.PureFunction;
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

    @PureFunction
    static Object convertToObject(String json, Method method) {
        if (isNotInconvertibleType(method.getReturnType())) {
            return json;
        }

        try {
            return convertToObject(json, method.getGenericReturnType());
        } catch (JsonProcessingException e) {
            throw new ClassConvertException(json, e);
        }
    }

    private static Object convertToObject(String json, Type genericType)
            throws JsonProcessingException {

        if (List.class.isAssignableFrom(genericType.getClass())) {
            JavaType targetType = JsonUtils.getObjectType(genericType.getClass());
            return JsonUtils.convertList(json, targetType);
        }

        JavaType targetType = JsonUtils.getObjectType(genericType);
        return JsonUtils.jsonToObject(json, targetType);
    }

    private static boolean isNotInconvertibleType(Class<?> returnType) {
        return !ObjectClasses.isInconvertibleClass(returnType);
    }
}
