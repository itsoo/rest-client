package com.cupshe.demo.service;

import com.cupshe.army.knife.ResponseVO;
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

    List<DemoDTO> findDemoList(DemoDTO dto);

    List<Integer> findIdList(DemoDTO dto);

    List<String> findTitleList(DemoDTO dto);

    ResponseVO<Map<String, List<DemoDTO>>> complexObject();
}
