package org.shiguang.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainPageController {

    /**
     * 访问根路径时重定向到主页
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    /**
     * 访问/dashboard时重定向到主页
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "index";
    }
} 