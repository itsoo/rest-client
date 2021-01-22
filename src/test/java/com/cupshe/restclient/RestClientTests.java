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
        DemoDTO dto = DemoDTO.defaultInstance();
        ResponseVO<Object> res = demoService.postForm(dto);
        System.out.println(res);
    }

    @Test
    public void testPostBody() {
        DemoDTO dto = DemoDTO.defaultInstance();
        DemoDTO res = demoService.postBody(dto);
        System.out.println(res);
    }

    @Test
    public void pathVariable() {
        DemoDTO dto = DemoDTO.defaultInstance();
        String res = demoService.pathVariable(dto.getId(), dto.getName());
        System.out.println(res);
    }

    @Test
    public void testDeleteById() {
        DemoDTO dto = DemoDTO.defaultInstance();
        demoService.deleteById(dto.getId());
    }

    @Test
    public void testFindDemoList() {
        DemoDTO dto = DemoDTO.defaultInstance();
        List<Map<String, List<DemoDTO>>> res = demoService.findDemoList(dto);
        System.out.println(res);
    }
}
