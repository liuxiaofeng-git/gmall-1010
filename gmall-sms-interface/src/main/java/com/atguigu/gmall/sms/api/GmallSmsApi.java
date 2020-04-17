package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;

import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallSmsApi {
    //保存营销信息的方法
    @PostMapping("sms/skubounds/skusale/save")
    @ApiOperation("保存营销信息的方法")
    public ResponseVo<Object> saveSkuSale(@RequestBody SkuSaleVo skuSaleVo);

    @GetMapping("sms/skubounds/sku/{skuId}")
    public ResponseVo<List<ItemSaleVo>> querySkuBySkuId(@PathVariable("skuId") Long skuId);
}
