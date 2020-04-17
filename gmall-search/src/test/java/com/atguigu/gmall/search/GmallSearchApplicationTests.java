package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.entity.Goods;
import com.atguigu.gmall.search.entity.SearchAttrValue;
import com.atguigu.gmall.search.feign.GmallPmsFeignClient;
import com.atguigu.gmall.search.feign.GmallWmsFeignClient;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private GmallWmsFeignClient wmsFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Test
    void contextLoads() {
        this.restTemplate.createIndex(Goods.class);
        this.restTemplate.putMapping(Goods.class);
    }

    @Test
    void importDataToElasticsearch() {
        Integer pageNum = 1;
        Integer pageSize = 100;
        do {
            PageParamVo pageParamVo = new PageParamVo();
            pageParamVo.setPageNum(pageNum);
            pageParamVo.setPageSize(pageSize);
            ResponseVo<List<SpuEntity>> spuResponseVo = pmsFeignClient.querySpusByPage(pageParamVo);
            List<SpuEntity> spuEntities = spuResponseVo.getData();
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsFeignClient.querySkusBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)) {
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();

                        //查询检索属性
                        ResponseVo<List<AttrEntity>> attrsResponseVo = this.pmsFeignClient.queryAttrsByCategoryId(skuEntity.getCategoryId(), null, 1);
                        List<AttrEntity> attrEntities = attrsResponseVo.getData();
                        if (!CollectionUtils.isEmpty(attrEntities)) {
                            List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                            ResponseVo<List<SpuAttrValueEntity>> spuAttrValuesResponseVo = this.pmsFeignClient.querySpuAttrValuesById(spuEntity.getId(), attrIds);
                            List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValuesResponseVo.getData();
                            ResponseVo<List<SkuAttrValueEntity>> skuAttrValuesResponseVo = this.pmsFeignClient.querySkuAttrValuesById(skuEntity.getId(), attrIds);
                            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValuesResponseVo.getData();
                            List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                            if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                                searchAttrValues.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                    SearchAttrValue searchAttrValue = new SearchAttrValue();
                                    BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValue);
                                    return searchAttrValue;
                                }).collect(Collectors.toList()));
                            }
                            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                                searchAttrValues.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                    SearchAttrValue searchAttrValue = new SearchAttrValue();
                                    BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValue);
                                    return searchAttrValue;
                                }).collect(Collectors.toList()));
                            }
                            goods.setAttrs(searchAttrValues);
                        }

                        //查询品牌信息
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsFeignClient.queryBrandById(skuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity!=null) {
                            goods.setBrandId(skuEntity.getBrandId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        // 查询分类信息
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsFeignClient.queryCategoryById(skuEntity.getCategoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity!=null) {
                            goods.setCategoryId(skuEntity.getCategoryId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                        //查询库存信息
                        ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsFeignClient.queryWareSkusBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b)->a+b).get());
                        }
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setCreateTime(spuEntity.getCreateTime());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSkuId(skuEntity.getId());
                        return goods;
                    }).collect(Collectors.toList());
                    this.goodsRepository.saveAll(goodsList);
                }
            });
            pageNum++;
            pageSize = spuEntities.size();
        } while (pageSize == 100);

    }
}
