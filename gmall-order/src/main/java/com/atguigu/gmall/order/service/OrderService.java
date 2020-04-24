package com.atguigu.gmall.order.service;

import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.vo.OrderConfirmVo;

public interface OrderService {
    OrderConfirmVo orderConfirm();

    void submit(OrderSubmitVo orderSubmitVo);
}
