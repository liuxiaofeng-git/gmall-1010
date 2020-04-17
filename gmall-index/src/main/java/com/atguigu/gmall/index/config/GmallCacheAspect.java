package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 切面类
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //获取切入点对象的方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法对象
        Method method = signature.getMethod();
        //获取方法的返回值类型
        Class<?> returnType = method.getReturnType();
        //获取切入点方法的参数
        String args = Arrays.asList(joinPoint.getArgs()).toString();
        //获取方法上的注解对象
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        //获取缓存前缀
        String keyPrefix = annotation.keyPrefix();
        //redis的key
        String key = keyPrefix + args;

        //前置通知
        //查询redis中是否有数据，有就返回，没有就查询并保存到redis
        String json = this.redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(json)) {
            return JSON.parseObject(json, returnType);
        }

        //为防止缓存击穿，需要增加分布式锁
        String lockKey = annotation.lockKey();
        RLock lock = this.redissonClient.getFairLock(lockKey + args);
        lock.lock();
        //再去缓存中查询一下是否有数据
        String jsonAgain = this.redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(jsonAgain)) {
            lock.unlock();
            return JSON.parseObject(jsonAgain, returnType);
        }

        //目标方法
        Object proceedResult = joinPoint.proceed(joinPoint.getArgs());

        //后置通知
        //如果查询到的数据为null，也放入缓存中，并设置低于3分钟的过期时间，防止缓存穿透
        int timeout = annotation.timeout();
        int random = annotation.random();
        if (proceedResult == null) {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(proceedResult), timeout, TimeUnit.MINUTES);
        } else {
            //设置过期时间,并将过期时间设置成随机值，防止缓存雪崩
            redisTemplate.opsForValue().set(key, JSON.toJSONString(proceedResult), timeout + new Random().nextInt(random), TimeUnit.MINUTES);
        }

        //释放锁
        lock.unlock();
        return proceedResult;
    }
}
