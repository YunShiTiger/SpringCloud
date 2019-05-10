package com.tigerjoys.com.tigerjoys.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 *  统一进行规范操作处理  如果是服务层的提供服务化的 一律使用RestController注解
 *  如果是对应的视图层控制器一律使用Controller注解
 */
@RestController
public class MemberApiController {

    @Value("${server.port}")
    private String serverPort;

    @RequestMapping("/getMember")
    public String getMember() {
        return "会员服务被调用了，调用的端口是:" + serverPort;
    }
}
