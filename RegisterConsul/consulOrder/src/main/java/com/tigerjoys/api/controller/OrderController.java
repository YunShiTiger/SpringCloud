package com.tigerjoys.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/orderToMember")
    public String orderToMember() {
        String memberUrl = "http://consul-member/member";
        return restTemplate.getForObject(memberUrl, String.class);
    }
}
