package com.atguigu.gmall.pms.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.client.utils.StringUtils;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.SmsFeignClient;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private SmsFeignClient smsFeignClient;

   @Autowired
   private SpuDescService spuDescService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpusPageByCategoryId(PageParamVo pageParamVo, Long categoryId) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        if (categoryId != 0) {
            wrapper.eq("category_id", categoryId);
        }
        String key = pageParamVo.getKey();
        if (StringUtil.isNotBlank(key)) {
            wrapper.and(t -> t.like("name", key).or().like("id", key));
        }
        return new PageResultVo(this.page(pageParamVo.getPage(), wrapper));
    }

    @Override
    @GlobalTransactional
    public void bigSave(SpuVo spuVo) {
        //1保存spu相关的信息
        //1.1保存spu基本信息
        saveSpu(spuVo);
        //1.2保存spu_desc表
        Long spuId =spuDescService.saveSpuDesc(spuVo);

       // int i=1/0;
        //1.3保存spu_attr_value
        saveSpuAttrValue(spuVo, spuId);
        //2保存sku相关的信息
        saveSku(spuVo, spuId);
        //int i=1/0;
    }

    @Override
    public void saveSku(SpuVo spuVo, Long spuId) {
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        skus.forEach(skuVo -> {
            SkuEntity skuEntity = new SkuEntity();
            BeanUtils.copyProperties(skuVo,skuEntity);
            skuEntity.setBrandId(spuVo.getBrandId());
            skuEntity.setCategoryId(spuVo.getCategoryId());
            skuEntity.setSpuId(spuId);
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                skuEntity.setDefaultImage(skuEntity.getDefaultImage() == null ? images.get(0) : skuEntity.getDefaultImage());
            }
            skuMapper.insert(skuEntity);
            //保存sku_images
            Long skuId = skuEntity.getId();
            if (!CollectionUtils.isEmpty(images)) {
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setSort(0);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(image, images.get(0)) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList());

                skuImagesService.saveBatch(skuImagesEntities);
            }
            //保存sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            saleAttrs.forEach(skuAttrValueEntity -> {
                skuAttrValueEntity.setSort(0);
                skuAttrValueEntity.setSkuId(skuId);
            });
           skuAttrValueService.saveBatch(saleAttrs);

           //保存skuVo中剩余的营销信息(远程调用feign)
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            smsFeignClient.saveSkuSale(skuSaleVo);

        });
    }

    @Override
    public void saveSpuAttrValue(SpuVo spuVo, Long spuId) {
        List<SpuAttrValueVo> spuAttrValueVoList = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(spuAttrValueVoList)) {
            List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueVoList.stream().map(spuAttrValueVo -> {
                spuAttrValueVo.setSpuId(spuId);
                spuAttrValueVo.setSort(0);
                return spuAttrValueVo;
            }).collect(Collectors.toList());
            spuAttrValueService.saveBatch(spuAttrValueEntities);
        }
    }



    @Override
    public void saveSpu(SpuVo spuVo) {
        spuVo.setPublishStatus(1);
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
    }

}