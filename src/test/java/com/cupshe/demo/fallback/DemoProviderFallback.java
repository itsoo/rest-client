package com.cupshe.demo.fallback;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.rpc.DemoProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DemoProviderFallback
 *
 * @author zxy
 */
public class DemoProviderFallback implements DemoProvider {

    @Override
    public ResponseVO<Object> postForm(DemoDTO dto) {
        return ResponseVO.of("fallback.");
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
    public List<DemoDTO> findDemoList(DemoDTO dto) {
        return Collections.singletonList(dto);
    }

    @Override
    public List<Integer> findIdList(DemoDTO dto) {
        return Collections.singletonList(dto.getAge());
    }

    @Override
    public List<String> findTitleList(DemoDTO dto) {
        return Collections.singletonList(dto.getName());
    }

    @Override
    public ResponseVO<Map<String, List<DemoDTO>>> complexObject() {
        return ResponseVO.of(new HashMap<>());
    }
}
