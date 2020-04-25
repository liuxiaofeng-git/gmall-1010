package com.atguigu.gmall.oms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;

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

    /**
     * 绑定交换机
     * @return
     */
//    @Bean
//    public Exchange exchange(){
//        return new TopicExchange("order.exchange",true,false,null);
//    }

    /**
     * 绑定延时队列
     * @return
     */
    @Bean
    public Queue ttlQueue(){
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","order.exchange");
        arguments.put("x-dead-letter-routing-key","order.dead");
        arguments.put("x-message-ttl",60000);
        return new Queue("order.ttl.queue",true,false,false,arguments);
    }

    /**
     * 将延时队列绑定到交换机
     * @return
     */
    @Bean
    public Binding ttlBinding(){
        return new Binding("order.ttl.queue",Binding.DestinationType.QUEUE,"order.exchange","order.ttl",null);
    }

    /**
     * 死信队列
     * @return
     */
    @Bean
    public Queue deadQueue(){

        return new Queue("order.dead.queue",true,false,false,null);
    }

    /**
     * 将死信队列绑定到交换机
     * @return
     */
    @Bean
    public Binding deadBinding(){
        return new Binding("order.dead.queue", Binding.DestinationType.QUEUE,"order.exchange","order.dead",null);
    }
}
