package com.atguigu.gmall.search.entity;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseAttrVo {

    private Long attrId;

    private String attrName;

    private List<String> attrValues;
}
