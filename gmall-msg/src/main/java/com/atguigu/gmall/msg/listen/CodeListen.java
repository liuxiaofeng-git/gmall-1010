package com.atguigu.gmall.msg.listen;

import com.atguigu.gmall.msg.utils.SmsTemplate;
import com.rabbitmq.client.Channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
public class CodeListen {

    @Autowired
    private SmsTemplate smsTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "msg.code.queue", durable = "true"),
            exchange = @Exchange(value = "msg.code.exchange", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = "msg.insert"))
    public void codeListen(Channel channel, Message message, Map<String, Object> msg) throws IOException {
        //发送短信
        Map<String, String> map = new HashMap<>();
        map.put("mobile", msg.get("phone").toString());
        map.put("param", "code:"+msg.get("code").toString());
        map.put("tpl_id", "TP1711063");
        boolean sendMessage = smsTemplate.sendMessage(map);
        if (sendMessage) {
            log.info("验证码发送成功");
        }else {
            log.error("验证码发送失败");
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
