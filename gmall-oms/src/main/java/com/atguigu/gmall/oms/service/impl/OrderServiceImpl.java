package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsFeignClient;
import com.atguigu.gmall.oms.feign.GmallSmsFeignClient;
import com.atguigu.gmall.oms.feign.GmallUmsFeignClient;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private GmallUmsFeignClient umsFeignClient;

    @Autowired
    private GmallSmsFeignClient smsFeignClient;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public OrderEntity saveOrder(OrderSubmitVo orderSubmitVo) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(orderSubmitVo.getUserId());
        orderEntity.setOrderSn(orderSubmitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());

        UserEntity userEntity = this.umsFeignClient.queryUserById(orderSubmitVo.getUserId()).getData();

        if (userEntity != null) {
            orderEntity.setUsername(userEntity.getUsername());
        }
        BigDecimal integrationAmount = new BigDecimal(orderSubmitVo.getBounds() / 100);
        BigDecimal freightAmount = new BigDecimal(8);
        orderEntity.setTotalAmount(orderSubmitVo.getTotalPrice());
        orderEntity.setPayAmount(orderSubmitVo.getTotalPrice().subtract(integrationAmount).add(freightAmount));
        orderEntity.setIntegrationAmount(integrationAmount);

        orderEntity.setPayType(orderSubmitVo.getPayType());
        orderEntity.setSourceType(orderSubmitVo.getSourceType());
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(orderSubmitVo.getDeliveryCompany());
        orderEntity.setDeliverySn(UUID.randomUUID().toString());//先随机生成一个
        orderEntity.setAutoConfirmDay(14);

        orderEntity.setIntegration(null);
        orderEntity.setGrowth(null);


        //TODO 发票


        UserAddressEntity userAddressEntity = orderSubmitVo.getAddress();
        if (userAddressEntity != null) {
            orderEntity.setReceiverName(userAddressEntity.getName());
            orderEntity.setReceiverPhone(userAddressEntity.getPhone());
            orderEntity.setReceiverPostCode(userAddressEntity.getPostCode());
            orderEntity.setReceiverProvince(userAddressEntity.getProvince());
            orderEntity.setReceiverCity(userAddressEntity.getCity());
            orderEntity.setReceiverRegion(userAddressEntity.getRegion());
            orderEntity.setReceiverAddress(userAddressEntity.getAddress());
        }

        orderEntity.setConfirmStatus(1);
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(orderSubmitVo.getBounds());

        this.orderMapper.insert(orderEntity);

        //保存订单详情
            List<OrderItemVo> items = orderSubmitVo.getItems();
            List<OrderItemEntity> orderItemEntities = items.stream().map(item -> {
                OrderItemEntity orderItemEntity = new OrderItemEntity();

                orderItemEntity.setOrderSn(orderSubmitVo.getOrderToken());
                orderItemEntity.setOrderId(orderEntity.getId());

                CompletableFuture<SkuEntity> skuEntityCompletableFuture = CompletableFuture.supplyAsync(() -> {
                    SkuEntity skuEntity = this.pmsFeignClient.querySkuById(item.getSkuId()).getData();
                    if (skuEntity != null) {
                        orderItemEntity.setSkuId(skuEntity.getId());
                        orderItemEntity.setSkuName(skuEntity.getName());
                        orderItemEntity.setSkuPic(skuEntity.getDefaultImage());
                        orderItemEntity.setSkuPrice(skuEntity.getPrice());
                        orderItemEntity.setSkuQuantity(item.getCount().intValue());
                        orderItemEntity.setCategoryId(skuEntity.getCategoryId());

                        CompletableFuture<Void> skuAttrValuesCompletableFuture = CompletableFuture.runAsync(() -> {
                            List<SkuAttrValueEntity> skuAttrValueEntities = this.pmsFeignClient.querySkuSaleAttrValuesBySkuId(skuEntity.getId()).getData();
                            orderItemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));
                        }, threadPoolExecutor);
                        CompletableFuture.allOf(skuAttrValuesCompletableFuture).join();
                    }
                    return skuEntity;
                }, threadPoolExecutor);

                CompletableFuture<Void> voidCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {

                    CompletableFuture<Void> spuCompletableFuture1 = CompletableFuture.runAsync(() -> {
                        SpuEntity spuEntity = this.pmsFeignClient.querySpuById(skuEntity.getSpuId()).getData();
                        if (spuEntity != null) {
                            orderItemEntity.setSpuId(spuEntity.getId());
                            orderItemEntity.setSpuName(spuEntity.getName());
                        }
                    }, threadPoolExecutor);

                    CompletableFuture<Void> spuDescCompletableFuture1 = CompletableFuture.runAsync(() -> {
                        SpuDescEntity spuDescEntity = this.pmsFeignClient.querySpuDescById(skuEntity.getSpuId()).getData();
                        if (spuDescEntity != null) {
                            orderItemEntity.setSpuPic(spuDescEntity.getDecript());
                        }
                    }, threadPoolExecutor);

                    CompletableFuture<Void> brandCompletableFuture1 = CompletableFuture.runAsync(() -> {
                        BrandEntity brandEntity = this.pmsFeignClient.queryBrandById(skuEntity.getBrandId()).getData();
                        if (brandEntity != null) {
                            orderItemEntity.setSpuBrand(brandEntity.getName());
                        }
                    }, threadPoolExecutor);

                    CompletableFuture<Void> skuBoundsCompletableFuture1 = CompletableFuture.runAsync(() -> {
                        SkuBoundsEntity skuBoundsEntity = this.smsFeignClient.queryBoundsByskuId(skuEntity.getId()).getData();
                        if (skuBoundsEntity != null) {
                            orderItemEntity.setGiftGrowth(skuBoundsEntity.getBuyBounds().intValue());
                            orderItemEntity.setGiftIntegration(skuBoundsEntity.getGrowBounds().intValue());
                        }
                    }, threadPoolExecutor);


                    CompletableFuture.allOf(spuCompletableFuture1, spuDescCompletableFuture1, brandCompletableFuture1, skuBoundsCompletableFuture1).join();
                }, threadPoolExecutor);

                CompletableFuture.allOf(voidCompletableFuture).join();
                return orderItemEntity;
            }).collect(Collectors.toList());

            this.orderItemService.saveBatch(orderItemEntities);

       // CompletableFuture.allOf().join();
        return orderEntity;
    }

}