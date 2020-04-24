package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
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
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CartListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    private static final String KEY_PREFIX = "cart:";
    private static final String PRICE_PREFIX = "cart:price:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart.pms.queue", durable = "true"),
            exchange = @Exchange(value = "pms.item.exchange", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"cart.update"}))
    public void cartListener(Long spuId, Channel channel, Message message) throws IOException {
        //根据spuId查询sku
        List<SkuEntity> skuEntities = this.pmsFeignClient.querySkusBySpuId(spuId).getData();
        if (CollectionUtils.isEmpty(skuEntities)) {
            return;
        }
        //价格同步
        skuEntities.forEach(skuEntity -> {
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
        });
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order.delete.queue", durable = "true"),
            exchange = @Exchange(value = "order.exchange", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.delete"}
    ))
    public void deleteCartListener(Map<String, Object> map, Channel channel, Message message) throws IOException {
        try {
            Long userId = (Long) map.get("userId");
            String skuIdsJson = map.get("skuIds").toString();
            List<String> skuIds = JSON.parseArray(skuIdsJson, String.class);
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            hashOps.delete(skuIds.toArray());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            if (message.getMessageProperties().isRedelivered()) {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            }else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,true);
            }
        }
    }
}
