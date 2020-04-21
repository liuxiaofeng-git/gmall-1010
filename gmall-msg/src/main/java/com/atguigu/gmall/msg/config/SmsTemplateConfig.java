package com.atguigu.gmall.msg.config;

import com.atguigu.gmall.msg.utils.SmsTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmsTemplateConfig {
    @Bean
    @ConfigurationProperties(prefix = "sms")
    public SmsTemplate smsTemplate(){
        return new SmsTemplate();
    }
}
