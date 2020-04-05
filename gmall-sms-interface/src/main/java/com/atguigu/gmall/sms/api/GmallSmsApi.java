package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;

import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallSmsApi {
    //保存营销信息的方法
    @PostMapping("sms/skubounds/skusale/save")
    @ApiOperation("保存营销信息的方法")
    public ResponseVo<Object> saveSkuSale(@RequestBody SkuSaleVo skuSaleVo);
}
