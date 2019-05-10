package com.tigerjoys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
//@EnableEurekaClient 将当前服务注册到eureka上
public class AppEurekaMember {

    public static void main(String[] args) {
        SpringApplication.run(AppEurekaMember.class,args);
    }
}
