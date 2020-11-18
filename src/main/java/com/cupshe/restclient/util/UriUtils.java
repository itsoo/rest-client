package com.cupshe.restclient.util;

import com.cupshe.ak.text.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * UriUtils
 *
 * @author zxy
 */
public class UriUtils {

    public static String encode(Object value) {
        return StringUtils.isEmpty(value) ? "" : encode(value.toString(), StandardCharsets.UTF_8);
    }

    public static String encode(String source, Charset charset) {
        return encodeUriComponent(source, charset);
    }

    private static String encodeUriComponent(String source, Charset charset) {
        if (StringUtils.isEmpty(source)) {
            return source;
        }

        byte[] bytes = source.getBytes(charset);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        boolean changed = false;
        for (byte b : bytes) {
            if (b < 0) {
                b += 256;
            }

            if (isAllowed(b)) {
                bos.write(b);
            } else {
                bos.write('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                bos.write(hex1);
                bos.write(hex2);
                changed = true;
            }
        }

        return (changed ? new String(bos.toByteArray(), charset) : source);
    }

    private static boolean isAllowed(int c) {
        return (isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
    }

    private static boolean isAlpha(int c) {
        return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(int c) {
        return (c >= '0' && c <= '9');
    }
}
