package com.atguigu.gmall.index.config;

import java.lang.annotation.*;

@Target({ElementType.METHOD})//作用在方法上
@Retention(RetentionPolicy.RUNTIME)//运行时起作用
@Inherited//可继承
@Documented//加到文档
public @interface GmallCache {
    /**
     * redis缓存的key前缀
     * @return
     */
    String keyPrefix() default "";

    /**
     * 分布式锁的key
     * @return
     */
    String lockKey() default "lock";

    /**
     * 缓存的过期时间
     * 单位：分钟
     * @return
     */
    int timeout() default 5;

    /**
     * 为防止缓存雪崩，设置的随机值
     * @return
     */
    int random() default 30;
}
