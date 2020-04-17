package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuFullReductionMapper skuFullReductionMapper;

    @Autowired
    private SkuLadderMapper skuLadderMapper;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    @Transactional
    public void saveSkuSale(SkuSaleVo skuSaleVo) {
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuSaleVo, skuBoundsEntity);
        List<Integer> work = skuSaleVo.getWork();
        if (!CollectionUtils.isEmpty(work)) {
            skuBoundsEntity.setWork(work.get(3) * 8 + work.get(2) * 4 + work.get(1) * 2 + work.get(0) * 1);
        }
        this.save(skuBoundsEntity);

        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        skuFullReductionMapper.insert(skuFullReductionEntity);

        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        skuLadderMapper.insert(skuLadderEntity);

    }

    @Override
    public List<ItemSaleVo> querySkuBySkuId(Long skuId) {
        //积分
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        ItemSaleVo itemSaleVo = new ItemSaleVo();
        itemSaleVo.setType("积分");
        itemSaleVo.setDesc("购买积分："+skuBoundsEntity.getBuyBounds()+"，成长积分："+skuBoundsEntity.getGrowBounds());

        //满减
        SkuFullReductionEntity skuFullReductionEntity = this.skuFullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        ItemSaleVo itemSaleVo1 = new ItemSaleVo();
        itemSaleVo1.setType("满减");
        itemSaleVo1.setDesc("满："+skuFullReductionEntity.getFullPrice()+"元，减"+skuFullReductionEntity.getReducePrice()+"元");

        //打折
        SkuLadderEntity skuLadderEntity = this.skuLadderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        ItemSaleVo itemSaleVo2 = new ItemSaleVo();
        itemSaleVo2.setType("打折");
        itemSaleVo2.setDesc("满："+skuLadderEntity.getFullCount()+"件，打"+skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");
        ArrayList<ItemSaleVo> itemSaleVos = new ArrayList<>();
        itemSaleVos.add(itemSaleVo);
        itemSaleVos.add(itemSaleVo1);
        itemSaleVos.add(itemSaleVo2);
        return itemSaleVos;
    }

}