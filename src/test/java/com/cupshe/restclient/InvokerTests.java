package com.cupshe.restclient;

import com.cupshe.Application;
import com.cupshe.demo.rpc.fallback.DemoProviderFallback;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.lang.reflect.Method;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = Application.class)
public class InvokerTests {

    @Test
    public void test() throws Throwable {
        Class<DemoProviderFallback> fallback = DemoProviderFallback.class;
        Method method = fallback.getMethod("pathVariable", Long.class, String.class);
        Object result = FallbackInvoker.of(fallback, method).invoke(getArgs(1L, "233"));
        System.out.println(result);
    }

    private Object[] getArgs(Object... args) {
        return args;
    }
}
