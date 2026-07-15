package com.suprith.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminViewController {

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/products")
    public String products() {
        return "admin/products";
    }

    @GetMapping("/admin/categories")
    public String categories() {
        return "admin/categories";
    }

    @GetMapping("/admin/brands")
    public String brands() {
        return "admin/brands";
    }

    @GetMapping("/admin/inventory")
    public String inventory() {
        return "admin/inventory";
    }

    @GetMapping("/admin/orders")
    public String orders() {
        return "admin/orders";
    }

    @GetMapping("/admin/coupons")
    public String coupons() {
        return "admin/coupons";
    }

    @GetMapping("/admin/users")
    public String users() {
        return "admin/users";
    }

    @GetMapping("/admin/reviews")
    public String reviews() {
        return "admin/reviews";
    }
}
