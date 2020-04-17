package com.atguigu.gmall.search.listern;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.entity.Goods;
import com.atguigu.gmall.search.entity.SearchAttrValue;
import com.atguigu.gmall.search.feign.GmallPmsFeignClient;
import com.atguigu.gmall.search.feign.GmallWmsFeignClient;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQChannel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ItemListern {

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private GmallWmsFeignClient wmsFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    /**
     * docker启动rabbitmq的方法"docker run -d --name rabbitmq --publish 5671:5671 --publish 5672:5672 --publish 4369:4369 --publish 25672:25672 --publish 15671:15671 --publish 15672:15672 rabbitmq:management"
     * @param spuId
     * @param channel
     * @param message
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.queue", durable = "true"),
            exchange = @Exchange(value = "pms.item.exchange", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.insert", "item.update"}))
    public void listernItem(Long spuId, Channel channel, Message message) throws IOException {
        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsFeignClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = skuResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntities)) {
            List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();

                //查询检索属性
                ResponseVo<List<AttrEntity>> attrsResponseVo = this.pmsFeignClient.queryAttrsByCategoryId(skuEntity.getCategoryId(), null, 1);
                List<AttrEntity> attrEntities = attrsResponseVo.getData();
                if (!CollectionUtils.isEmpty(attrEntities)) {
                    List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                    ResponseVo<List<SpuAttrValueEntity>> spuAttrValuesResponseVo = this.pmsFeignClient.querySpuAttrValuesById(spuId, attrIds);
                    List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValuesResponseVo.getData();
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValuesResponseVo = this.pmsFeignClient.querySkuAttrValuesById(skuEntity.getId(), attrIds);
                    List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValuesResponseVo.getData();
                    List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                        searchAttrValues.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValue);
                            return searchAttrValue;
                        }).collect(Collectors.toList()));
                    }
                    if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                        searchAttrValues.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValue);
                            return searchAttrValue;
                        }).collect(Collectors.toList()));
                    }
                    goods.setAttrs(searchAttrValues);
                }

                //查询品牌信息
                ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsFeignClient.queryBrandById(skuEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResponseVo.getData();
                if (brandEntity != null) {
                    goods.setBrandId(skuEntity.getBrandId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }

                // 查询分类信息
                ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsFeignClient.queryCategoryById(skuEntity.getCategoryId());
                CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                if (categoryEntity != null) {
                    goods.setCategoryId(skuEntity.getCategoryId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                //查询库存信息
                ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsFeignClient.queryWareSkusBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                    goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                }
                SpuEntity spuEntity = this.pmsFeignClient.querySpuById(spuId).getData();
                if (spuEntity != null) {
                    goods.setCreateTime(spuEntity.getCreateTime());
                }
                goods.setPrice(skuEntity.getPrice().doubleValue());
                goods.setDefaultImage(skuEntity.getDefaultImage());
                goods.setTitle(skuEntity.getTitle());
                goods.setSkuId(skuEntity.getId());
                return goods;
            }).collect(Collectors.toList());
            this.goodsRepository.saveAll(goodsList);
        }

        //确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
