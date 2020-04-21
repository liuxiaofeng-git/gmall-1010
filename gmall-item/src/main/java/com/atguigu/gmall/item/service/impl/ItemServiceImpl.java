package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsFeignClient;
import com.atguigu.gmall.item.feign.GmallSmsFeignClient;
import com.atguigu.gmall.item.feign.GmallWmsFeignClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemCategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private GmallSmsFeignClient smsFeignClient;

    @Autowired
    private GmallWmsFeignClient wmsFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public ItemVo load(Long skuId) {
        ItemVo itemVo = new ItemVo();
        //异步编排开启多线程提高响应速度
        CompletableFuture<SkuEntity> skuEntityCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //查询出sku对象
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsFeignClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return null;
            }
            //设置sku
            itemVo.setSkuTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setSkuId(skuId);
            return skuEntity;
        }, threadPoolExecutor);

        //分类
        CompletableFuture<Void> categoriesCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemCategoryVo>> categoriesByCid3 = this.pmsFeignClient.queryCategoriesByCid3(skuEntity.getCategoryId());
            List<ItemCategoryVo> categoryVos = categoriesByCid3.getData();
            if (!CollectionUtils.isEmpty(categoryVos)) {
                itemVo.setCategories(categoryVos);
            }
        }, threadPoolExecutor);

        //品牌
        CompletableFuture<Void> brandCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsFeignClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        //spu信息
        CompletableFuture<Void> spuCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsFeignClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        //营销信息
        CompletableFuture<Void> salesCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsFeignClient.querySkuBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, threadPoolExecutor);

        //库存
        CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareSkusResponseVo = this.wmsFeignClient.queryWareSkusBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkusResponseVo.getData();
            if (wareSkuEntities != null) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        //需要sku所属spu下的所有sku的营销信息
        CompletableFuture<Void> skuAttrValuesCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<AttrValueVo>> skuAttrValuesResponseVo = this.pmsFeignClient.querySkuAttrValuesBySpuId(skuEntity.getSpuId());
            List<AttrValueVo> attrValueVos = skuAttrValuesResponseVo.getData();
            if (!CollectionUtils.isEmpty(attrValueVos)) {
                itemVo.setSaleAttrs(attrValueVos);
            }
        }, threadPoolExecutor);

        //sku的图片信息
        CompletableFuture<Void> skuImagesCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> skuImagesResponseVo = this.pmsFeignClient.querySkuImagesBySkuId(skuId);
            List<SkuImagesEntity> imagesEntities = skuImagesResponseVo.getData();
            itemVo.setImages(imagesEntities);
        }, threadPoolExecutor);

        //spu海报信息
        CompletableFuture<Void> spuDescCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsFeignClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

        //分组及组下的规格参数
        CompletableFuture<Void> groupsCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupsResponseVo = this.pmsFeignClient.queryGroupsWithAttrValues(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVos = groupsResponseVo.getData();
            itemVo.setGroups(itemGroupVos);
        }, threadPoolExecutor);

        //返回前阻塞等待全部查询结束一起返回
        CompletableFuture.allOf(skuEntityCompletableFuture, categoriesCompletableFuture, brandCompletableFuture,
                spuCompletableFuture, salesCompletableFuture, wareCompletableFuture, skuAttrValuesCompletableFuture,
                skuImagesCompletableFuture, spuDescCompletableFuture, groupsCompletableFuture).join();
        return itemVo;
    }
}
