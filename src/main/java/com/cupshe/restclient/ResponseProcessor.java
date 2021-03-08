package com.cupshe.restclient;

import com.cupshe.ak.json.JsonUtils;
import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.restclient.exception.ClassConvertException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

/**
 * ResponseProcessor
 *
 * @author zxy
 */
class ResponseProcessor {

    static final ResponseEntity<byte[]> REQUEST_TIMEOUT = new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);

    static String convertToString(@NonNull byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static Object convertToObject(@NonNull String json, Method method) {
        if (isNotInconvertibleType(method.getReturnType())) {
            return json;
        }

        try {
            return convertToObject(json, method.getGenericReturnType());
        } catch (JsonProcessingException e) {
            throw new ClassConvertException(json, e);
        }
    }

    private static Object convertToObject(String json, Type genericType) throws JsonProcessingException {
        JavaType targetType = JsonUtils.getObjectType(genericType);
        if (isFutureClass(genericType)) {
            targetType = targetType.containedType(0);
        }

        if (isNotInconvertibleType(getGenericClass(targetType))) {
            return json;
        }

        return JsonUtils.jsonToObject(json, targetType);
    }

    private static boolean isNotInconvertibleType(Class<?> returnType) {
        return !ObjectClasses.isInconvertibleClass(returnType);
    }

    private static boolean isFutureClass(Type genericType) {
        return Future.class.isAssignableFrom(getGenericClass(genericType));
    }

    private static Class<?> getGenericClass(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) genericType).getRawType();
        }

        if (genericType instanceof JavaType) {
            return ((JavaType) genericType).getRawClass();
        }

        return genericType.getClass();
    }
}
