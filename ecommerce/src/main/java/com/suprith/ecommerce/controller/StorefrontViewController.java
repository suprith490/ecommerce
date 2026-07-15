package com.suprith.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StorefrontViewController {

    @GetMapping("/")
    public String home() {
        return "store/home";
    }

    @GetMapping("/login")
    public String login() {
        return "store/login";
    }

    @GetMapping("/register")
    public String register() {
        return "store/register";
    }

    @GetMapping("/products")
    public String products() {
        return "store/products";
    }

    @GetMapping("/products/{slug}")
    public String productDetail() {
        return "store/product-detail";
    }

    @GetMapping("/cart")
    public String cart() {
        return "store/cart";
    }

    @GetMapping("/wishlist")
    public String wishlist() {
        return "store/wishlist";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "store/checkout";
    }

    @GetMapping("/profile")
    public String profile() {
        return "store/profile";
    }

    @GetMapping("/orders")
    public String orders() {
        return "store/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail() {
        return "store/order-detail";
    }
}
