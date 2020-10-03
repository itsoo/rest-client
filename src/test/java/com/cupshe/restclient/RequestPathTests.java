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
public class RequestPathTests {

    @Test
    public void test() {
        System.out.println(RequestProcessor.processStandardUri("abc", "abc"));
        System.out.println(RequestProcessor.processStandardUri("abc", "/abc"));
        System.out.println(RequestProcessor.processStandardUri("/abc", "abc"));
        System.out.println(RequestProcessor.processStandardUri("", "/abc"));
        System.out.println(RequestProcessor.processStandardUri("/", "abc"));
        System.out.println(RequestProcessor.processStandardUri("/abc", "/abc"));
        System.out.println(RequestProcessor.processStandardUri("/", "/abc"));
        System.out.println(RequestProcessor.processStandardUri("/", "/"));
        System.out.println(RequestProcessor.processStandardUri("/abc/", "/abc/"));
        System.out.println(RequestProcessor.processStandardUri("///", "//abc///"));
    }
}
