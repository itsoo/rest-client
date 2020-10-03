package com.cupshe.demo.service;

import com.cupshe.army.knife.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.rpc.DemoProvider;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * CommentServiceImpl
 *
 * @author zxy
 */
@Service
public class DemoServiceImpl implements DemoService {

    @Resource
    private DemoProvider demoProvider;

    @Override
    public ResponseVO<Object> postForm(DemoDTO dto) {
        return demoProvider.postForm(dto);
    }

    @Override
    public DemoDTO postBody(DemoDTO dto) {
        return demoProvider.postBody(dto);
    }

    @Override
    public String pathVariable(Long id, String title) {
        return demoProvider.pathVariable(id, title);
    }

    @Override
    public void deleteById(Long id) {
        demoProvider.deleteById(id);
    }

    @Override
    public List<DemoDTO> findDemoList(DemoDTO dto) {
        return demoProvider.findDemoList(dto);
    }

    @Override
    public List<Integer> findIdList(DemoDTO dto) {
        return demoProvider.findIdList(dto);
    }

    @Override
    public List<String> findTitleList(DemoDTO dto) {
        return demoProvider.findTitleList(dto);
    }

    @Override
    public ResponseVO<Map<String, List<DemoDTO>>> complexObject() {
        return demoProvider.complexObject();
    }

    public ResponseVO<Object> fallback() {
        return ResponseVO.of("test fallback method.");
    }
}
