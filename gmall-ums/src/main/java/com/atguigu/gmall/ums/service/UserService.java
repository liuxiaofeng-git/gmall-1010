package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserEntity;

import java.util.Map;

/**
 * 用户表
 *
 * @author liuxiaofeng
 * @email xfliu@atguigu.com
 * @date 2020-04-18 20:18:00
 */
public interface UserService extends IService<UserEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    Boolean check(String data, Integer type);

    void register(UserEntity userEntity, Integer code);

    UserEntity queryUser(String username, String password);

    void sendCode(String phone);
}

