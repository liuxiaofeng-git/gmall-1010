package com.atguigu.gmall.auth.controller;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsFeignClient;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.utils.CookieUtils;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("auth")
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("accredit")
    public ResponseVo<Object> accredit(@RequestParam("username") String username,
                                       @RequestParam("password") String password,
                                       HttpServletRequest request, HttpServletResponse response) {
        String jwt = this.authService.accredit(username, password);
        //设置到cookie中
        CookieUtils.setCookie(request, response, jwtProperties.getCookieName(), jwt, jwtProperties.getExpire() * 60);
        return ResponseVo.ok();
    }
}
