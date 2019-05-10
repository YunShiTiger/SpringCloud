package com.tigerjoys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
// @EnableDiscoveryClient 作用是 如果服务使用connsul、zookeeper 使用
// @EnableDiscoveryClient 向注册中心上注册服务
@EnableDiscoveryClient
public class ZkMember {

    public static void main(String[] args) {
        SpringApplication.run(ZkMember.class,args);
    }

}
