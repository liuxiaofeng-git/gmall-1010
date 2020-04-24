package com.atguigu.gmall.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuLockVo {
    private Long skuId;//商品id
    private BigDecimal count;//锁定数量
    private Long wareSkuId;//锁定成功时的锁定仓库id
    private Boolean lock;//锁定状态
    private String orderToken;//订单唯一的id，防重复提交
}
