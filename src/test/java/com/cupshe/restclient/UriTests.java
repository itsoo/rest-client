package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UriTests
 *
 * @author zxy
 */
public class UriTests {

    @Test
    public void testUris() {
        List<Kv> kvs = new ArrayList<>();
        kvs.add(new Kv("id", 1));
        kvs.add(new Kv("age", 20));
        kvs.add(new Kv("name", "ZhangSan"));
        String s = RequestProcessor.processPathVariableOf("/{ id }/abc/{ name}/xyz/{age }", kvs);
        System.out.println(s);
    }

    @Test
    public void testFormParams() {
        List<Kv> kvs = new ArrayList<>();
        kvs.add(new Kv("id", 1));
        kvs.add(new Kv("age", 20));
        kvs.add(new Kv("name", "Zhang San"));

        List<Map<String, Efg>> efgs = new ArrayList<>();
        Map<String, Efg> efg = new HashMap<>();
        efg.put("ppt", new Efg(999));
        efgs.add(efg);
        kvs.add(new Kv("efgs", new Abc(efgs)));

        MultiValueMap<String, Object> map = RequestProcessor.convertObjectToMultiValueMap("kvs", kvs);
        System.out.println(map);
        System.out.println("================================================");
        String s = RequestProcessor.convertObjectToQueryUri("kvs", kvs);
        System.out.println(s);
        System.out.println("================================================");
        String s1 = RequestProcessor.processRequestParamOf("http://127.0.0.1:8080?t=123&abc=1", kvs);
        System.out.println(s1);
    }

    @Test
    public void testStringUtils() {
        System.out.println(StringUtils.trimTrailingCharacter("abc&&&", '&'));
    }

    @Data
    @AllArgsConstructor
    private static class Abc {
        private List<Map<String, Efg>> efgs;
    }

    @Data
    @AllArgsConstructor
    private static class Efg {
        private Integer ppt;
    }
}
