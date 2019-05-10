package com.tigerjoys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
// @EnableDiscoveryClient 作用是 如果服务使用connsul、zookeeper 使用
// @EnableDiscoveryClient 向注册中心上注册服务
@EnableDiscoveryClient
public class ZkOrder {

    public static void main(String[] args) {
        SpringApplication.run(ZkOrder.class,args);
    }

    // 默认rest方式开启 负载均衡功能 如果以app-itmayiedu-member名称进行调用服务接口的时候 必须
    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
