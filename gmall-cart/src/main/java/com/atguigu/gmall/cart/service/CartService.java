package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.entity.Cart;

import java.util.List;

public interface CartService {
    void addItemToCart(Cart cart);

    List<Cart> queryCart();

    void update(Cart cart);

    void check(Cart cart);

    void delete(Long skuId);

    List<Cart> queryCheckCartsByUserId(Long userId);
}
