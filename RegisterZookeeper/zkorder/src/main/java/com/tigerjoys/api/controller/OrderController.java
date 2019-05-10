package com.tigerjoys.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    //如何获取到注册中心上服务列表信息
    //提供用于进行访问注册中心中的对象
    @Autowired
    private DiscoveryClient discoveryClient;

    // springcloud 中使用那些技术实现调用服务接口 feign 或者rest
    @RequestMapping("/orderToMember")
    public String orderToMember() {
        String memberUrl = "http://zk-member/getMember";
        return restTemplate.getForObject(memberUrl, String.class);
    }

    @RequestMapping("/discoveryClientMember")
    public List<ServiceInstance> discoveryClientMember() {
        List<ServiceInstance> instances = discoveryClient.getInstances("zk-member");
        for (ServiceInstance serviceInstance : instances) {
            System.out.println("url:" + serviceInstance.getUri());
        }
        return instances;
    }
}
