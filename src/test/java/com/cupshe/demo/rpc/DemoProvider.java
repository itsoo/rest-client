package com.cupshe.demo.rpc;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.rpc.fallback.DemoProviderFallback;
import com.cupshe.restclient.lang.RestClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DemoProvider
 *
 * @author zxy
 */
@RestClient(name = "comment", path = "/api/v1/comment", maxAutoRetries = 3,
        fallback = DemoProviderFallback.class, readTimeout = 1000)
public interface DemoProvider {

    @PostMapping("/form")
    ResponseVO<Object> postForm(DemoDTO dto);

    @PostMapping("/body")
    DemoDTO postBody(@RequestBody DemoDTO dto);

    @GetMapping("/{id}")
    String pathVariable(@PathVariable("id") Long id, @RequestParam("title") String title);

    @DeleteMapping("/{id}")
    void deleteById(@PathVariable("id") Long id/*, @RequestHeader("token") String token*/);

    @GetMapping("/demo-list")
    ArrayList<Map<String, List<DemoDTO>>> findDemoList(@RequestParam("dtos") ArrayList<Map<String, List<DemoDTO>>> dtos);

    @PostMapping("/demo-list-post")
    ArrayList<Map<String, List<DemoDTO>>> findDemoListPost(@RequestParam("dtos") ArrayList<Map<String, List<DemoDTO>>> dtos);

    @GetMapping("/age-list")
    List<Integer> findIdList(DemoDTO dto);

    @GetMapping("/name-list")
    List<String> findTitleList(DemoDTO dto);

    @GetMapping("/sbigobject")
    ResponseVO<Map<String, List<DemoDTO>>> complexObject();
}
