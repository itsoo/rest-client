package com.cupshe.restclient;

import com.cupshe.restclient.lang.HttpsSupported;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * MethodClassAnnotationTests
 *
 * @author zxy
 */
public class MethodClassAnnotationTests {

    @Test
    public void test() throws Throwable {
        Method abcMain = Abc.class.getDeclaredMethod("main");
        System.out.println(abcMain.getDeclaringClass());
        HttpsSupported abcAnn = AnnotationUtils.findAnnotation(abcMain.getDeclaringClass(), HttpsSupported.class);
        Assert.assertNotNull(abcAnn);

        Method bcdMain = Bcd.class.getDeclaredMethod("main");
        System.out.println(bcdMain);
        HttpsSupported bcdAnn = AnnotationUtils.findAnnotation(bcdMain, HttpsSupported.class);
        Assert.assertNotNull(bcdAnn);
    }

    /**
     * test annotation class
     */
    @HttpsSupported
    public static class Abc {

        public void main() {}
    }

    /**
     * test annotation method
     */
    public static class Bcd {

        @HttpsSupported
        public void main() {}
    }
}
