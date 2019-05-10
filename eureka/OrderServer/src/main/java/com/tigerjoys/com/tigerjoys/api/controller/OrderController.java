package com.tigerjoys.com.tigerjoys.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    // 订单服务调用会员服务
    @RequestMapping("/getOrder")
    public String getOrder() {
        // 有两种方式，一种是采用服务别名方式调用，另一种是直接调用 使用别名去注册中心上获取对应的服务调用地址
        String url = "http://memberServer/getMember";
        String result = restTemplate.getForObject(url, String.class);
        System.out.println("订单服务调用会员服务result:" + result);
        return result;
    }
}
