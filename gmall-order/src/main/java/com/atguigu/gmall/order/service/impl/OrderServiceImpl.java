package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.entity.UserInfo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;

import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private GmallSmsFeignClient smsFeignClient;

    @Autowired
    private GmallUmsFeignClient umsFeignClient;

    @Autowired
    private GmallWmsFeignClient wmsFeignClient;

    @Autowired
    private GmallCartFeignClient cartFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private GmallOmsFeignClient omsFeignClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "order:token:";

    @Override
    public OrderConfirmVo orderConfirm() {
        //使用异步编排
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        //获取用户登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        //送货清单
        CompletableFuture<List<Cart>> cartsCompletableFuture = CompletableFuture.supplyAsync(() -> {
            List<Cart> carts = this.cartFeignClient.queryCheckCartsByUserId(userId).getData();
            if (CollectionUtils.isEmpty(carts)) {
                throw new OrderException("还没有商品加入购物车！");
            }
            return carts;
        }, threadPoolExecutor);

        CompletableFuture<Void> itemsCompletableFuture = cartsCompletableFuture.thenAcceptAsync(carts -> {
            List<OrderItemVo> orderItemVos = carts.stream().map(cart -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                orderItemVo.setSkuId(cart.getSkuId());
                orderItemVo.setCount(new BigDecimal(cart.getCount()));

                //根据skuId查询sku
                CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsFeignClient.querySkuById(cart.getSkuId());
                    SkuEntity skuEntity = skuEntityResponseVo.getData();
                    if (skuEntity != null) {
                        orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                        orderItemVo.setTitle(skuEntity.getTitle());
                        orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
                        orderItemVo.setPrice(skuEntity.getPrice());
                    }
                }, threadPoolExecutor);

                //根据skuId查询销售属性
                CompletableFuture<Void> skuAttrValuesCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<SkuAttrValueEntity> skuAttrValues = this.pmsFeignClient.querySkuSaleAttrValuesBySkuId(cart.getSkuId()).getData();
                    if (!CollectionUtils.isEmpty(skuAttrValues)) {
                        orderItemVo.setSkuAttrValues(skuAttrValues);
                    }
                }, threadPoolExecutor);

                //根据skuId查询营销信息
                CompletableFuture<Void> salesCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<ItemSaleVo> itemSaleVos = this.smsFeignClient.querySkuBySkuId(cart.getSkuId()).getData();
                    if (!CollectionUtils.isEmpty(itemSaleVos)) {
                        orderItemVo.setItemSaleVos(itemSaleVos);
                    }
                }, threadPoolExecutor);

                //根据skuId查询库存
                CompletableFuture<Void> wareSkuCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<WareSkuEntity> wareSkuEntities = this.wmsFeignClient.queryWareSkusBySkuId(cart.getSkuId()).getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                    }
                }, threadPoolExecutor);

                CompletableFuture.allOf(wareSkuCompletableFuture, salesCompletableFuture,
                        skuAttrValuesCompletableFuture, skuCompletableFuture).join();

                return orderItemVo;
            }).collect(Collectors.toList());
            orderConfirmVo.setOrderItems(orderItemVos);
        }, threadPoolExecutor);


        //地址
        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(() -> {
            List<UserAddressEntity> addressEntities = this.umsFeignClient.queryAddressesByUserId(userId).getData();
            if (!CollectionUtils.isEmpty(addressEntities)) {
                orderConfirmVo.setAddresses(addressEntities);
            }
        }, threadPoolExecutor);

        //积分
        CompletableFuture<Void> boundsCompletableFuture = CompletableFuture.runAsync(() -> {
            UserEntity userEntity = this.umsFeignClient.queryUserById(userId).getData();
            if (userEntity != null) {
                orderConfirmVo.setBounds(userEntity.getIntegration());
            }
        }, threadPoolExecutor);

        //防重的唯一标识
        CompletableFuture<Void> orderTokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            orderConfirmVo.setOrderToken(orderToken);
            //redis中也保存一份
            this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken);
        }, threadPoolExecutor);

        CompletableFuture.allOf(itemsCompletableFuture, addressCompletableFuture, boundsCompletableFuture, orderTokenCompletableFuture).join();
        return orderConfirmVo;
    }

    @Override
    @Transactional
    public OrderEntity submit(OrderSubmitVo orderSubmitVo) {
        Long userId = LoginInterceptor.getUserInfo().getUserId();
        List<OrderItemVo> orderItems = orderSubmitVo.getItems();
        String orderToken = orderSubmitVo.getOrderToken();

        //防重
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Boolean execute = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!execute) {
            throw new OrderException("提交频率过快，请勿重复提交");
        }
//        CompletableFuture.runAsync(() -> {
//        }, threadPoolExecutor);

        //验总价
        if (CollectionUtils.isEmpty(orderItems)) {
            throw new OrderException("您还未勾选商品，请选择要购买的商品");
        }
        BigDecimal currentPrice = orderItems.stream().map(orderItemVo -> {
            SkuEntity skuEntity = this.pmsFeignClient.querySkuById(orderItemVo.getSkuId()).getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(orderItemVo.getCount());
        }).reduce((a, b) -> a.add(b)).get();
        if (orderSubmitVo.getTotalPrice().compareTo(currentPrice) != 0) {
            throw new OrderException("页面已过期，刷新后再试！");
        }
//        CompletableFuture.runAsync(() -> {
//        }, threadPoolExecutor);

        //验库存并锁定库存
        List<SkuLockVo> skuLockVos = orderItems.stream().map(orderItemVo -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(orderItemVo.getSkuId());
            skuLockVo.setCount(orderItemVo.getCount());
            skuLockVo.setOrderToken(orderToken);
            return skuLockVo;
        }).collect(Collectors.toList());
        List<SkuLockVo> skuLockVoList = this.wmsFeignClient.checkAndLock(skuLockVos).getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)) {
            throw new OrderException("手慢了，库存不足：" + JSON.toJSONString(skuLockVoList));
        }
//        CompletableFuture.runAsync(() -> {
//        }, threadPoolExecutor);

        //下单，生成订单
        OrderEntity orderEntity =null;
        try {
            orderSubmitVo.setUserId(userId);
            orderEntity = this.omsFeignClient.save(orderSubmitVo).getData();
        } catch (Exception e) {
            //释放锁定库存
            this.rabbitTemplate.convertAndSend("order.exchange", "stock.unlock", orderToken);
            throw new OrderException("订单保存异常，请稍候再试");
        }
//        CompletableFuture.runAsync(() -> {
//        }, threadPoolExecutor);

        //删除购物车记录
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        List<Long> skuIds = orderItems.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("order.exchange", "order.delete", map);
//        CompletableFuture.runAsync(() -> {
//        }, threadPoolExecutor);

        return orderEntity;
    }

}
