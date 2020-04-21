package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import sun.nio.ch.FileKey;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Data
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String pubKeyPath;//公钥路径

    private String priKeyPath;//私钥路径

    private String secret;//盐

    private Integer expire;//cookie过期时间

    private String cookieName;//cookie名称

    private PublicKey publicKey;//公钥

    private PrivateKey privateKey;//私钥

    @PostConstruct
    public void init() {
        File pubFile = new File(pubKeyPath);
        File priFile = new File(priKeyPath);
        try {
            if (!pubFile.exists() || !priFile.exists()) {
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            //e.printStackTrace();
            log.error("公钥私钥初始化失败！原因：{}", e.getMessage());
        }
    }
}
