package com.cupshe.demo.domain;

import lombok.Data;

/**
 * DemoDTO
 *
 * @author zxy
 */
@Data
public class DemoDTO {

    private Long id;

    private String name;

    private Integer age;

    public static DemoDTO defaultInstance() {
        DemoDTO result = new DemoDTO();
        result.setId(1L);
        result.setName("zhang 三");
        result.setAge(18);
        return result;
    }
}
