package com.cupshe.restclient.util;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.text.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ObjectClassUtils
 *
 * @author zxy
 */
public class ObjectClassUtils {

    public static List<Kv> getObjectProperties(Object object) {
        if (object == null) {
            return Collections.emptyList();
        }

        Field[] fields = object.getClass().getDeclaredFields();
        if (fields.length == 0) {
            return Collections.emptyList();
        }

        List<Kv> result = new ArrayList<>();
        for (Field f : fields) {
            result.add(new Kv(f.getName(), getFieldValueByName(f.getName(), object)));
        }

        return result;
    }

    public static Object getFieldValueByName(String filedName, Object o) {
        try {
            String getter = getterMethodName(filedName);
            Method method = o.getClass().getDeclaredMethod(getter);
            if (isNotPublic(method) && !method.isAccessible()) {
                method.setAccessible(true);
            }

            return method.invoke(o);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getShortNameAsProperty(String qualifier) {
        if (qualifier == null) {
            return null;
        }

        int i = qualifier.lastIndexOf('.');
        if (i < 0) {
            return StringUtils.lowerFirstLetter(qualifier);
        }

        return StringUtils.lowerFirstLetter(qualifier.substring(i + 1));
    }

    public static boolean isInconvertibleClass(Class<?> clazz) {
        return !clazz.isPrimitive()
                && !clazz.isAssignableFrom(Boolean.class)
                && !clazz.isAssignableFrom(Character.class)
                && !clazz.isAssignableFrom(Byte.class)
                && !clazz.isAssignableFrom(Short.class)
                && !clazz.isAssignableFrom(Integer.class)
                && !clazz.isAssignableFrom(Long.class)
                && !clazz.isAssignableFrom(Float.class)
                && !clazz.isAssignableFrom(Double.class)
                && !clazz.isAssignableFrom(Void.class)
                && !clazz.isAssignableFrom(String.class)
                && !clazz.isAnnotation();
    }

    private static String getterMethodName(String fieldName) {
        return "get" + StringUtils.upperFirstLetter(fieldName);
    }

    private static boolean isNotPublic(Method method) {
        return !Modifier.isPublic(method.getModifiers())
                || !Modifier.isPublic(method.getDeclaringClass().getModifiers());
    }
}
