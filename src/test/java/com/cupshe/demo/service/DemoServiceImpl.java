package com.cupshe.demo.service;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.rpc.DemoProvider;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * CommentServiceImpl
 *
 * @author zxy
 */
@Service
public class DemoServiceImpl implements DemoService {

    @Autowired
    private DemoProvider demoProvider;

    @SneakyThrows
    @Override
    public ResponseVO<Object> postForm(DemoDTO dto) {
        return demoProvider.postForm(dto).get();
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
    public List<Map<String, List<DemoDTO>>> findDemoList(DemoDTO dto) {
        return demoProvider.findDemoList();
    }
}
