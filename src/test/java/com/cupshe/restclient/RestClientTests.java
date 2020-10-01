package com.cupshe.restclient;

import com.cupshe.Application;
import com.cupshe.army.knife.ResponseVO;
import com.cupshe.demo.service.DemoService;
import com.cupshe.demo.domain.DemoDTO;
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

    // TODO:
    //  1. RPC 接口请求参数携带 @RequestBody，以及无注解参数的传值，另外需测试表单提交方式
    //  2. @GetMapping，@PostMapping，@PutMapping，@DeleteMapping，@RequestMapping
    //  3. 返回值类型为 ResponseVO<T>，List<T>，Map<K, V>，Object 其它对象类型
    //  4. 返回值类型为基础数据类型（包装类及 String）
    //  5. 无返回值类型的请求场景（接口方法返回值类型为 void）
    //  6. 无请求参数的请求场景（接口方法无入参）
    //  7. 测试本工程打包为 jar 文件后与各工程的整合，及联调是否可以正常工作
}
