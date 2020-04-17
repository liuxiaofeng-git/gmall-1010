package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;

import java.util.List;

/**
 * 商品spu积分设置
 *
 * @author liuxiaofeng
 * @email xfliu@atguigu.com
 * @date 2020-03-31 21:22:37
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    void saveSkuSale(SkuSaleVo skuSaleVo);

    List<ItemSaleVo> querySkuBySkuId(Long skuId);
}

