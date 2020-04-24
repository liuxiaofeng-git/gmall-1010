package com.atguigu.gmall.ums.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GmallUmsApi {

    @GetMapping("ums/user/query")
    public ResponseVo<UserEntity> queryUser(@RequestParam("username") String username,
                                            @RequestParam("password") String password);

    @PostMapping("ums/user/register")
    public ResponseVo<Object> register(UserEntity userEntity, @RequestParam("code") Integer code);

    @GetMapping("ums/user/check/{data}/{type}")
    public ResponseVo<Boolean> check(@PathVariable("data") String data, @PathVariable("type") Integer type);

    @GetMapping("ums/user/code")
    public ResponseVo<Object> sendCode(@RequestParam("phone")String phone);

    @GetMapping("ums/useraddress/user/{userId}")
    public ResponseVo<List<UserAddressEntity>> queryAddressesByUserId(@PathVariable("userId") Long userId);

    @GetMapping("ums/user/{id}")
    public ResponseVo<UserEntity> queryUserById(@PathVariable("id") Long id);
}
