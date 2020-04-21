package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Data
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String pubKeyPath;//公钥路径

    private String cookieName;//cookie名称

    private PublicKey publicKey;//公钥


    @PostConstruct
    public void init() {
        File pubFile = new File(pubKeyPath);
        try {
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            //e.printStackTrace();
            log.error("公钥获取失败！原因：{}", e.getMessage());
        }
    }
}
