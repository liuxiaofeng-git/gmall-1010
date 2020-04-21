package com.atguigu.gmall.auth.service.impl;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsFeignClient;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private GmallUmsFeignClient umsFeignClient;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public String accredit(String username, String password) {
        //根据用户名和密码查询用户
        ResponseVo<UserEntity> userEntityResponseVo = this.umsFeignClient.queryUser(username, password);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity == null) {
            throw new UserException("请重新登录");
        }
        //生成jwt
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId",userEntity.getId());
            map.put("username",userEntity.getUsername());
            String jwt = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            return jwt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
