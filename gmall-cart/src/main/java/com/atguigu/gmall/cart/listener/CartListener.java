package com.atguigu.gmall.cart.listener;

import com.atguigu.gmall.cart.feign.GmallPmsFeignClient;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Component
public class CartListener {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    private static final String PRICE_PREFIX = "cart:price:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart.pms.queue",durable = "true"),
            exchange = @Exchange(value = "pms.item.exchange",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"cart.update"}))
    public void cartListener(Long spuId, Channel channel, Message message) throws IOException {
        //根据spuId查询sku
        List<SkuEntity> skuEntities = this.pmsFeignClient.querySkusBySpuId(spuId).getData();
        if (CollectionUtils.isEmpty(skuEntities)) {
            return;
        }
        //价格同步
        skuEntities.forEach(skuEntity -> {
            this.redisTemplate.opsForValue().set(PRICE_PREFIX+skuEntity.getId(),skuEntity.getPrice().toString());
        });
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
