package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class OrderItemVo {
    private Long skuId;
    private String title;
    private String defaultImage;
    private BigDecimal weight;
    private BigDecimal price;//加入购物车时的价格
    private BigDecimal count;
    private List<SkuAttrValueEntity> skuAttrValues;//销售属性
    private List<ItemSaleVo> itemSaleVos;//营销信息
    private Boolean store=false;//是否有货
}
