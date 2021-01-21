package com.cupshe.demo.rpc;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.rpc.fallback.DemoProviderFallback;
import com.cupshe.restclient.lang.RestClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * DemoProvider
 *
 * @author zxy
 */
@RestClient(name = "comment", path = "/api/v1/comment", maxAutoRetries = 3,
        fallback = DemoProviderFallback.class, readTimeout = 1000)
public interface DemoProvider {

    @PostMapping("/form")
    Future<ResponseVO<Object>> postForm(DemoDTO dto);

    @PostMapping("/body")
    DemoDTO postBody(@RequestBody DemoDTO dto);

    @GetMapping("/{id}")
    String pathVariable(@PathVariable("id") Long id, @RequestParam("title") String title);

    @DeleteMapping("/{id}")
    void deleteById(@PathVariable("id") Long id);

    @GetMapping("/demo-list")
    List<Map<String, List<DemoDTO>>> findDemoList();
}
