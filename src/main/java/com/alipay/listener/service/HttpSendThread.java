package com.alipay.listener.service;

import com.alipay.listener.model.ConfigModel;
import com.alipay.listener.model.OrderModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.text.DecimalFormat;
import java.text.MessageFormat;

public class HttpSendThread extends Thread {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final OrderModel orderModel;

    public HttpSendThread(OrderModel orderModel) {
        this.orderModel = orderModel;
    }

    public void run()
    {
        ConfigService configService = new ConfigService();
        ConfigModel configModel = configService.getConfig();
        if(configModel == null || StringUtils.isEmpty(configModel.getApi())){
            logger.warn("未配置API参数");
            return;
        }
        logger.info(MessageFormat.format("开始 POST 订单 {0} 到 {1}",orderModel.getId(),configModel.getApi()));
        try {
            MultiValueMap<String,String> valueMap = new LinkedMultiValueMap<>();
            valueMap.add("orderId",orderModel.getId());
            valueMap.add("amount",orderModel.getAmount().toString());
            valueMap.add("account",orderModel.getAccount());
            valueMap.add("name",orderModel.getName());
            valueMap.add("createdTime",orderModel.getCreatedTime());
            String key = MessageFormat.format("{0}|{1}|{2}|{3}|{4}|{5}",orderModel.getId(),String.format("%.2f",orderModel.getAmount()),orderModel.getAccount(),orderModel.getName(),orderModel.getCreatedTime(),"itellyou");
            valueMap.add("key", DigestUtils.md5DigestAsHex(key.getBytes("UTF-8")));

            String body = HttpService.post(configModel.getApi(), valueMap);
            body = body.substring(1,body.length() -1);
            String[] results = body.split("\\|");
            if (results[0].trim().equals("1")) {
                logger.info(MessageFormat.format("订单 {0} POST 成功", orderModel.getId()));
            }else{
                logger.info(MessageFormat.format("订单 {0} POST 失败，{1}", orderModel.getId(),results[1]));
            }
        }catch (Exception e){
            logger.error(e.getLocalizedMessage());
        }
    }
}
