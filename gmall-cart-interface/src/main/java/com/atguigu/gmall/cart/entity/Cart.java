package com.atguigu.gmall.cart.entity;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Cart {
    private Long skuId;
    private Boolean store;//是否有货
    private String defaultImage;
    private String title;
    private List<SkuAttrValueEntity> saleAttrs;//所有的销售属性
    private BigDecimal price;//加入购物车时的价格
    private BigDecimal currentPrice;//当前最新价格
    private Integer count;
    private Boolean check;//选中状态
    private List<ItemSaleVo> sales;//营销信息：满减，积分，打折
}
