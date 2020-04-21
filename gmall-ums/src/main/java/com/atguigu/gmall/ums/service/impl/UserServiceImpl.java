package com.atguigu.gmall.ums.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.ScwAppUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static final String KEY_PREFIX = "code:ums:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean check(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type) {
            case 1:
                queryWrapper.eq("username", data);
                break;
            case 2:
                queryWrapper.eq("phone", data);
                break;
            case 3:
                queryWrapper.eq("email", data);
                break;
            default:
                return null;
        }
        return this.userMapper.selectCount(queryWrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, Integer code) {
        //获取redis中的短信验证码
        String checkCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + userEntity.getPhone());
        if (!StringUtil.equals(checkCode, code.toString())) {
            return;
        }
        userEntity.setGrowth(0);
        userEntity.setLevelId(1L);
        userEntity.setCreateTime(new Date());
        userEntity.setStatus(1);
        //生成盐
        String salt = StringUtils.replace(UUID.randomUUID().toString().substring(0, 6), "-", "");
        userEntity.setSalt(salt);
        userEntity.setPassword(DigestUtils.md5DigestAsHex((salt + DigestUtils.md5DigestAsHex(userEntity.getPassword().getBytes())).getBytes()));
        int i = this.userMapper.insert(userEntity);
        System.err.println(i);
        if (i == 1) {
            //删除redis中的短信验证码
            this.redisTemplate.delete(KEY_PREFIX + userEntity.getPhone());
        }
    }

    @Override
    public UserEntity queryUser(String username, String password) {
        UserEntity userEntity = this.userMapper.selectOne(new QueryWrapper<UserEntity>().eq("username", username));
        if (userEntity == null) {
            throw new UserException("用户名不存在");
        }
        password = DigestUtils.md5DigestAsHex((userEntity.getSalt() + DigestUtils.md5DigestAsHex(password.getBytes())).getBytes());
        if (!StringUtil.equals(password, userEntity.getPassword())) {
            throw new UserException("密码错误");
        }
        return userEntity;
    }

    @Override
    public void sendCode(String phone) {
        //生成验证码
        Integer code = new Random().nextInt(999999);
        //判断是否是手机号码格式
        if (!ScwAppUtils.isMobilePhone(phone)) {
            return;
        }
        //保存一份到redis
        this.redisTemplate.opsForValue().set(KEY_PREFIX + phone, code.toString(), RandomUtils.nextLong(30,60), TimeUnit.MINUTES);
        //发送消息给rabbitMQ,携带手机号和验证码
        HashMap<String, Object> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        rabbitTemplate.convertAndSend("msg.code.exchange", "msg.insert", msg);

    }

}