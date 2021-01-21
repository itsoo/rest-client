package com.cupshe.demo.service;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;

import java.util.List;
import java.util.Map;

/**
 * DemoService
 *
 * @author zxy
 */
public interface DemoService {

    ResponseVO<Object> postForm(DemoDTO dto);

    DemoDTO postBody(DemoDTO dto);

    String pathVariable(Long id, String title);

    void deleteById(Long id);

    List<Map<String, List<DemoDTO>>> findDemoList(DemoDTO dto);
}
