package com.cupshe.demo.rpc;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.restclient.RestClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DemoProvider
 *
 * @author zxy
 */
@RestClient(value = "comment", path = "/api/v1/comment", maxAutoRetries = 3,
        fallback = "@com.cupshe.demo.service.DemoServiceImpl#fallback", readTimeout = 1000)
public interface DemoProvider {

    @PostMapping("/form")
    ResponseVO<Object> postForm(DemoDTO dto);

    @PostMapping("/body")
    DemoDTO postBody(@RequestBody DemoDTO dto);

    @GetMapping("/{id}")
    String pathVariable(@PathVariable("id") Long id, @RequestParam("title") String title);

    @DeleteMapping("/{id}")
    void deleteById(@PathVariable("id") Long id);

    @GetMapping("/demo-list")
    List<DemoDTO> findDemoList(DemoDTO dto);

    @GetMapping("/age-list")
    List<Integer> findIdList(DemoDTO dto);

    @GetMapping("/name-list")
    List<String> findTitleList(DemoDTO dto);

    @GetMapping("/sbigobject")
    ResponseVO<Map<String, List<DemoDTO>>> complexObject();
}
