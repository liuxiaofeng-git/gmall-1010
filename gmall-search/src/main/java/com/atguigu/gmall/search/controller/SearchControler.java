package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.entity.SearchParam;
import com.atguigu.gmall.search.entity.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("search")
public class SearchControler {

    @Autowired
    private SearchService searchService;
    @GetMapping
    public ResponseVo<SearchResponseVo> search(SearchParam searchParam){
        SearchResponseVo searchResponseVo = this.searchService.search(searchParam);
        return ResponseVo.ok(searchResponseVo);
    }
}
