package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author liuxiaofeng
 * @email xfliu@atguigu.com
 * @date 2020-03-31 19:58:53
 */
public interface SkuAttrValueService extends IService<SkuAttrValueEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<AttrValueVo> querySkuAttrValuesBySpuId(Long spuId);

    List<SkuAttrValueEntity> querySkuSaleAttrValuesBySkuId(Long skuId);
}

