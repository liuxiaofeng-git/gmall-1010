package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.entity.SearchParam;
import com.atguigu.gmall.search.entity.SearchResponseVo;

public interface SearchService {
    //查询的方法
    SearchResponseVo search(SearchParam searchParam);
}
