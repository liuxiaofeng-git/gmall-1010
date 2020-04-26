package com.atguigu.gmall.order.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.config.AlipayTemplate;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @GetMapping("confirm")
    public ResponseVo<OrderConfirmVo> orderConfirm() {
        OrderConfirmVo orderConfirmVo = this.orderService.orderConfirm();
        return ResponseVo.ok(orderConfirmVo);
    }


    @PostMapping(value = "submit")
    public ResponseVo<Object> submit(@RequestBody OrderSubmitVo orderSubmitVo) {
        OrderEntity orderEntity = this.orderService.submit(orderSubmitVo);
        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderEntity.getOrderSn());
        payVo.setTotal_amount(orderEntity.getPayAmount().toString());
        payVo.setSubject("谷粒商城支付接口");
        payVo.setBody("");
        try {
            String form = this.alipayTemplate.pay(payVo);
            return ResponseVo.ok(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return ResponseVo.fail();
        }
    }



    /**
     * 支付宝支付成功后的回调方法
     *
     * @return
     */
    @PostMapping("alipay/success")
    public ResponseVo<Object> paySuccess() {
        System.out.println("支付成功");

        return ResponseVo.ok();
    }
}
