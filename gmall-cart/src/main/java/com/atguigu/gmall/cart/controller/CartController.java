package com.atguigu.gmall.cart.controller;


import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("cart")
@RestController
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("check/{userId}")
    public ResponseVo<List<Cart>> queryCheckCartsByUserId(@PathVariable("userId") Long userId) {
        List<Cart> carts = this.cartService.queryCheckCartsByUserId(userId);
        return ResponseVo.ok(carts);
    }

    @PostMapping("save")
    public ResponseVo<Object> addItemToCart(@RequestBody Cart cart) {
        this.cartService.addItemToCart(cart);
        return ResponseVo.ok();
    }

    @PostMapping("query")
    public ResponseVo<List<Cart>> queryCart() {
        List<Cart> carts = this.cartService.queryCart();
        return ResponseVo.ok(carts);
    }

    @PostMapping("update")
    public ResponseVo<Object> update(@RequestBody Cart cart) {
        this.cartService.update(cart);
        return ResponseVo.ok();
    }

    @PostMapping("check")
    public ResponseVo<Object> check(@RequestBody Cart cart) {
        this.cartService.check(cart);
        return ResponseVo.ok();
    }

    @DeleteMapping("delete/{skuId}")
    public ResponseVo<Object> delete(@PathVariable("skuId") Long skuId) {
        this.cartService.delete(skuId);
        return ResponseVo.ok();
    }
}
