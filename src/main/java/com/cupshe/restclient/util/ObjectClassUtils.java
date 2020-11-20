package com.cupshe.restclient.util;

import com.cupshe.ak.core.Kv;
import com.cupshe.ak.text.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ObjectClassUtils
 *
 * @author zxy
 */
public class ObjectClassUtils {

    public static List<Kv> getObjectProperties(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }

        Field[] fields = obj.getClass().getDeclaredFields();
        if (fields.length == 0) {
            return Collections.emptyList();
        }

        List<Kv> result = new ArrayList<>();
        for (Field f : fields) {
            result.add(new Kv(f.getName(), getFieldValueByName(f.getName(), obj)));
        }

        return result;
    }

    public static Object getFieldValueByName(String filedName, Object o) {
        try {
            Method method = o.getClass().getDeclaredMethod(getterMethodName(filedName));

            try {
                method.setAccessible(true);
                return method.invoke(o);
            } finally {
                method.setAccessible(false);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String getterMethodName(String fieldName) {
        return "get" + StringUtils.upperFirstLetter(fieldName);
    }

    public static String getBeanName(String qualifier) {
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
}
