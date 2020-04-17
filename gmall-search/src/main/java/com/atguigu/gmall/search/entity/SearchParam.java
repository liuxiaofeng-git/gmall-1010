package com.atguigu.gmall.search.entity;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {

    private String keyword;//用户搜索的关键字

    private List<Long> brandId;//品牌的id，可多选

    private Long categoryId;//分类的id，可多选

    private List<String> props;//规格参数 props=33:3000-4000-5000,34:5-6-7

    private String order;//排序 冒号前是排序字段（0：得分 1：价格  2：销量 3：新品） 冒号后是升降序

    private Boolean store;//是否有货

    private Double priceFrom;//价格下限
    private Double priceTo;//价格上限

    private Integer pageNum=1;//默认当前第一页
    private final Integer pageSize=20;//每页显示20条数据，用户不允许修改


}
