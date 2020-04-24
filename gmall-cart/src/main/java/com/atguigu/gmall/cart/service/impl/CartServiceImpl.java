package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsFeignClient;
import com.atguigu.gmall.cart.feign.GmallSmsFeignClient;
import com.atguigu.gmall.cart.feign.GmallWmsFeignClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private GmallSmsFeignClient smsFeignClient;

    @Autowired
    private GmallWmsFeignClient wmsFeignClient;


    private static final String KEY_PREFIX = "cart:";
    private static final String PRICE_PREFIX = "cart:price:";

    @Override
    public void addItemToCart(Cart cart) {
        //获取用户对象
        String key = generateKey();
        //获取用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        //判断购物车中是否包含此商品
        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            //有，更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);//反序列化
            cart.setCount(cart.getCount() + count);
        } else {
            //没有，新增此商品
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsFeignClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                cart.setTitle(skuEntity.getTitle());
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setPrice(skuEntity.getPrice());
                cart.setCurrentPrice(skuEntity.getPrice());
            }
            ResponseVo<List<SkuAttrValueEntity>> saleAttrValuesResponseVo = pmsFeignClient.querySkuSaleAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValuesResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                cart.setSaleAttrs(skuAttrValueEntities);
            }
            List<ItemSaleVo> itemSaleVos = smsFeignClient.querySkuBySkuId(cart.getSkuId()).getData();
            if (!CollectionUtils.isEmpty(itemSaleVos)) {
                cart.setSales(itemSaleVos);
            }
            List<WareSkuEntity> wareSkuEntities = wmsFeignClient.queryWareSkusBySkuId(cart.getSkuId()).getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            cart.setCheck(true);
            //将数据库的最新价格同步到redis一份
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());
        }
        hashOps.put(skuId, JSON.toJSONString(cart)); //序列化保存到redis
    }

    @Override
    public List<Cart> queryCart() {
        //获取用户信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        String userKey = userInfo.getUserKey();
        //获取未登录的游客购物车
        String unLoginKey = KEY_PREFIX + userKey;
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);
        //未登录直接返回游客购物车
        List<Object> unLoginCartsJson = unLoginHashOps.values();
        //反序列化
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(unLoginCartsJson)) {
            unLoginCarts = unLoginCartsJson.stream().map(unLoginCartJson -> {
                Cart cart = JSON.parseObject(unLoginCartJson.toString(), Cart.class);
                //redis中查询最新的价格
                String price = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(price));
                return cart;
            }).collect(Collectors.toList());
        }
        if (userId == null) {
            return unLoginCarts;
        }
        //登录了，判断登录的购物车中是否包含未登录时的商品，合并购物车
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            //遍历未登录的购物车
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                if (loginHashOps.hasKey(skuId)) {
                    //有包含的，修改登录的购物车中的数量
                    String loginCartJson = loginHashOps.get(skuId).toString();
                    Integer count = cart.getCount();
                    //反序列化
                    cart = JSON.parseObject(loginCartJson, Cart.class);
                    cart.setCount(cart.getCount() + count);
                }
                //没有包含，添加进登录购物车中
                loginHashOps.put(skuId, JSON.toJSONString(cart));
            });
        }

        //删除未登录的游客购物车
        this.redisTemplate.delete(unLoginKey);

        //返回合并后的购物车
        List<Object> cartsJson = loginHashOps.values();
        if (CollectionUtils.isEmpty(cartsJson)) {
            return null;
        }
        return cartsJson.stream().map(cartJson -> {
            Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
            String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
            cart.setCurrentPrice(new BigDecimal(currentPrice));
            return cart;
        }).collect(Collectors.toList());
    }

    @Override
    public void update(Cart cart) {
        String key = generateKey();
        String skuId = cart.getSkuId().toString();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId)) {
            Integer count = cart.getCount();
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(skuId, JSON.toJSONString(cart));
        }
    }

    @Override
    public void check(Cart cart) {
        String key = generateKey();
        String skuId = cart.getSkuId().toString();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId)) {
            Boolean check = cart.getCheck();
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCheck(check);
            hashOps.put(skuId, JSON.toJSONString(cart));
        }
    }

    @Override
    public void delete(Long skuId) {
        String key = generateKey();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())) {
            hashOps.delete(skuId.toString());
        }
    }

    @Override
    public List<Cart> queryCheckCartsByUserId(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> cartsJson = hashOps.values();
        if (CollectionUtils.isEmpty(cartsJson)) {
            return null;
        }
       return cartsJson.stream().map(cartJson-> JSON.parseObject(cartJson.toString(), Cart.class)).filter(Cart::getCheck).collect(Collectors.toList());
    }

    private String generateKey() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String key = KEY_PREFIX;
        //判断用户是否登录
        if (userInfo.getUserId() == null) {
            //未登录
            key += userInfo.getUserKey();
        } else {
            key += userInfo.getUserId();
        }
        return key;
    }
}
