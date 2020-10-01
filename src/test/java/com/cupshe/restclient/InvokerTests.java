package com.cupshe.restclient;

import com.cupshe.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = Application.class)
public class InvokerTests {

    @Test
    public void test() throws Throwable {
        Object result = FallbackInvoker.of("@com.cupshe.demo.CommentServiceImpl#fallback").invoke();
        System.out.println(result);
    }
}
