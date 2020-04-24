package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("confirm")
    public ResponseVo<OrderConfirmVo> orderConfirm(){
        OrderConfirmVo orderConfirmVo= this.orderService.orderConfirm();
        return ResponseVo.ok(orderConfirmVo);
    }

    @PostMapping("submit")
    public ResponseVo<Object> submit(@RequestBody OrderSubmitVo orderSubmitVo){
        this.orderService.submit(orderSubmitVo);
        return ResponseVo.ok();
    }
}
