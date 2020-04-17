package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemCategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVo {
    // 分类no
    private List<ItemCategoryVo> categories;


    // 品牌ok
    private Long brandId;
    private String brandName;

    // spu信息ok
    private Long spuId;
    private String spuName;

    // sku相关信息ok
    private Long skuId;
    private String skuTitle;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;

    private List<ItemSaleVo> sales;  // 营销信息no

    private Boolean store = false; // 是否有货ok

    // 需要sku所属spu下的所有sku的营销信息no
    private List<AttrValueVo> saleAttrs;

    private List<SkuImagesEntity> images; // sku的图片信息ok

    private List<String> spuImages; // spu海报信息ok

    private List<ItemGroupVo> groups; // 分组及组下的规格参数no
}
