package com.tigerjoys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableEurekaClient
public class AppEurekaOrder {

    public static void main(String[] args) {
        SpringApplication.run(AppEurekaOrder.class,args);
    }

    //解决调用时找不到RestTemplate的错误 即需要将RestTemplate注入到容器中
    //如果使用rest方式以别名方式进行调用依赖ribbon负载均衡器 需要加入对应的自动引用注解@LoadBalanced
    //@LoadBalanced就能让这个RestTemplate在请求时拥有客户端负载均衡的能力  即开启负载均衡的能力
    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
