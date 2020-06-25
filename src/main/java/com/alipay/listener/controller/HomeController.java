package com.alipay.listener.controller;

import com.alipay.listener.service.AlipayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final AlipayService alipayService;

    public HomeController(AlipayService alipayService) {
        this.alipayService = alipayService;
        alipayService.start();
    }

    @GetMapping("/")
    public String index(){
        return alipayService.prevRefreshTime.toString();
    }
}
