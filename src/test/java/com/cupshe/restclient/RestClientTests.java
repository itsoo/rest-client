package com.cupshe.restclient;

import com.cupshe.Application;
import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.service.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = Application.class)
public class RestClientTests {

    @Autowired
    private DemoService demoService;

    @Test
    public void testPostForm() {
        DemoDTO dto = new DemoDTO();
        dto.setAge(12);
        dto.setName("zhang %20$3san");
        ResponseVO<Object> res = demoService.postForm(dto);
        System.out.println(res);
    }

    @Test
    public void testPostBody() {
        DemoDTO dto = new DemoDTO();
        dto.setAge(12);
        dto.setName("zhang %20$3san");
        DemoDTO res = demoService.postBody(dto);
        System.out.println(res);
    }

    @Test
    public void pathVariable() {
        String res = demoService.pathVariable(2L, "zhang %20$3san");
        System.out.println(res);
    }

    @Test
    public void testDeleteById() {
        demoService.deleteById(2L);
        System.out.println("success!");
    }

    @Test
    public void testFindDemoList() {
        DemoDTO dto = new DemoDTO();
        dto.setAge(12);
        dto.setName("zhang %20$3san");
        List<DemoDTO> res = demoService.findDemoList(dto);
        System.out.println(res);
    }

    @Test
    public void testFindIdList() {
        DemoDTO dto = new DemoDTO();
        dto.setAge(12);
        List<Integer> res = demoService.findIdList(dto);
        System.out.println(res);
    }

    @Test
    public void testFindTitleList() {
        DemoDTO dto = new DemoDTO();
        dto.setName("zhang %20$3san");
        List<String> res = demoService.findTitleList(dto);
        System.out.println(res);
    }

    @Test
    public void testComplexObject() {
        ResponseVO<Map<String, List<DemoDTO>>> res = demoService.complexObject();
        System.out.println(res);
    }
}
