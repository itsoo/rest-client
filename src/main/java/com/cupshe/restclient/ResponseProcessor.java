package com.cupshe.restclient;

import com.cupshe.ak.json.JsonUtils;
import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.restclient.exception.ClassConvertException;
import com.cupshe.restclient.lang.PureFunction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * ResponseProcessor
 *
 * @author zxy
 */
@PureFunction
class ResponseProcessor {

    static ResponseEntity<byte[]> defaultResponseEntity() {
        // gateway-timeout
        return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
    }

    static String convertToString(@Nullable byte[] bytes) {
        return Objects.nonNull(bytes)
                ? new String(bytes, StandardCharsets.UTF_8)
                : null;
    }

    static Object convertToObject(String json, Method method) {
        if (Objects.isNull(json)) {
            return null;
        }

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
