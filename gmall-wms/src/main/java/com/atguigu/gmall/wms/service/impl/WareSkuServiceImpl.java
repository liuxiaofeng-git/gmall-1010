package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "store:lock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> skuLockVos) {
        //判断传递的集合是否为空
        if (CollectionUtils.isEmpty(skuLockVos)) {
            return null;
        }

        //遍历验库存并锁库存
        skuLockVos.forEach(skuLockVo -> {
            this.checkLock(skuLockVo);
        });
        //如果有一个商品锁定失败，所有锁定成功的商品需要解锁，回滚
        List<SkuLockVo> successLocks = skuLockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
        List<SkuLockVo> failLocks = skuLockVos.stream().filter(skuLockVo -> !skuLockVo.getLock()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(failLocks)) {
            //解锁已经成功锁定的库存
            successLocks.forEach(successLock -> {
                this.wareSkuMapper.unLockStock(successLock.getWareSkuId(), successLock.getCount().intValue());
            });
            return skuLockVos;
        }
        //将锁定成功的商品保存在redis中
        String orderToken = skuLockVos.get(0).getOrderToken();
        this.redisTemplate.opsForValue().set(LOCK_PREFIX + orderToken, JSON.toJSONString(skuLockVos));
        return null;
    }

    private void checkLock(SkuLockVo skuLockVo) {
        RLock fairLock = redissonClient.getFairLock("lock:" + skuLockVo.getSkuId());
        fairLock.lock();
        //验库存
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.checkStock(skuLockVo.getSkuId(), skuLockVo.getCount().intValue());
        if (CollectionUtils.isEmpty(wareSkuEntities)) {
            skuLockVo.setLock(false);
            fairLock.unlock();
            return;
        }
        //锁库存
        if (this.wareSkuMapper.lockStock(wareSkuEntities.get(0).getId(), skuLockVo.getCount().intValue()) == 1) {
            skuLockVo.setLock(true);
            skuLockVo.setWareSkuId(wareSkuEntities.get(0).getId());
        } else {
            skuLockVo.setLock(true);
        }
        fairLock.unlock();
    }
}