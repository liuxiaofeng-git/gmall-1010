package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallOmsApi {
    @PostMapping("oms/order")
    public ResponseVo<OrderEntity> save(@RequestBody OrderSubmitVo orderSubmitVo);

    @GetMapping("oms/orderitem/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<OrderItemEntity> queryOrderItemById(@PathVariable("id") Long id);
}
