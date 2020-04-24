package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    private List<UserAddressEntity> addresses;//用户地址集合
    private Integer bounds;//抵扣积分
    private List<OrderItemVo> orderItems;//购物清单
    private String orderToken;//防重复提交定单的唯一订单号
}
