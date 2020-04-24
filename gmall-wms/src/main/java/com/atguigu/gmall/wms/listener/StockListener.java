package com.atguigu.gmall.wms.listener;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Component
public class StockListener {
    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "store:lock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order.stock.queue", durable = "true"),
            exchange = @Exchange(value = "order.exchange", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unLockListener(String orderToken, Channel channel, Message message) throws IOException {
        try {
            String lockJson = this.redisTemplate.opsForValue().get(LOCK_PREFIX + orderToken);
            if (StringUtil.isNotBlank(lockJson)) {
                List<SkuLockVo> lockVos = JSON.parseArray(lockJson, SkuLockVo.class);
                lockVos.forEach(lockVo -> {
                    this.wareSkuMapper.unLockStock(lockVo.getWareSkuId(), lockVo.getCount().intValue());
                });
                this.redisTemplate.delete(LOCK_PREFIX + orderToken);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
