package com.atguigu.gmall.item.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(@Value("${thread.pool.corePoolSize}") Integer corePoolSize,
                                                 @Value("${thread.pool.maximumPoolSize}") Integer maximumPoolSize,
                                                 @Value("${thread.pool.keepAliveTime}") Long keepAliveTime,
                                                 @Value("${thread.pool.blockQueueSize}") Integer blockQueueSize) {
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<>(blockQueueSize));
    }
}
