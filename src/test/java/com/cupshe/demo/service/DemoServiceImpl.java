package com.cupshe.demo.service;

import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.rpc.DemoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CommentServiceImpl
 *
 * @author zxy
 */
@Service
public class DemoServiceImpl implements DemoService {

    @Autowired
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
    public ArrayList<Map<String, List<DemoDTO>>> findDemoList(DemoDTO dto) {
        Map<String, List<DemoDTO>> map = new HashMap<>(2);
        map.put("demo", Collections.singletonList(dto));
        return demoProvider.findDemoList(new ArrayList<>(Collections.singletonList(map)));
//        return demoProvider.findDemoListPost(new ArrayList<>(Collections.singletonList(map)));
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
}
