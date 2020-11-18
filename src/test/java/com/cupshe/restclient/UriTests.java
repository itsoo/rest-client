package com.cupshe.restclient;

import com.cupshe.ak.core.Kv;
import com.cupshe.restclient.util.BeanUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;
import org.springframework.util.MultiValueMap;

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

        MultiValueMap<String, Object> map = RequestProcessor.convertObjectToMultiValueMap(kvs);
        System.out.println(map);
        System.out.println("================================================");
        String s = RequestProcessor.convertObjectToQueryUrl(kvs);
        System.out.println(s);
    }

    @Test
    public void testClasses() {
        System.out.println(BeanUtils.isInconvertibleClass(String.class));
        System.out.println(BeanUtils.isInconvertibleClass(Integer.class));
        System.out.println(BeanUtils.isInconvertibleClass(Abc.class));
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
