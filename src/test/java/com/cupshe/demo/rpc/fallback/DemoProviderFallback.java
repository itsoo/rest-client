package com.cupshe.demo.rpc.fallback;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.rpc.DemoProvider;
import com.cupshe.restclient.lang.Fallback;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * DemoProviderFallback
 *
 * @author zxy
 */
@Fallback
public class DemoProviderFallback implements DemoProvider {

    @Value("${fallback.tips}")
    private String fallbackTips;

    @Override
    public Future<ResponseVO<Object>> postForm(DemoDTO dto) {
        return Executors.newSingleThreadExecutor().submit(() -> ResponseVO.of(fallbackTips));
    }

    @Override
    public DemoDTO postBody(DemoDTO dto) {
        return dto;
    }

    @Override
    public String pathVariable(Long id, String title) {
        return "str(" + id + '-' + title + ')';
    }

    @Override
    public void deleteById(Long id) {
        System.out.println("fallback.");
    }

    @Override
    public List<Map<String, List<DemoDTO>>> findDemoList() {
        return null;
    }
}
