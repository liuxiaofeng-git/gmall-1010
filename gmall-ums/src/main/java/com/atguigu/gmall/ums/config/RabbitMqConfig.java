package com.atguigu.gmall.ums.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitMqConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        this.rabbitTemplate.setReturnCallback(this);
        this.rabbitTemplate.setConfirmCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean b, String s) {
       if (b) {
        log.info("消息已经到达交换机");
       }else {
           log.error("发送失败：消息未到达交换机！");
       }
    }

    @Override
    public void returnedMessage(Message message, int i, String s, String s1, String s2) {
        log.error("发送失败：消息未到达队列！",message.getBody());
    }
}
