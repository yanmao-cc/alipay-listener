package com.alipay.listener.service;

import com.alipay.listener.model.OrderModel;

import java.util.LinkedList;
import java.util.List;

public class LocalOrderThread extends Thread {

    private final LocalOrderService orderService;

    private List<OrderModel> orderModels = new LinkedList<>();
    public LocalOrderThread(List<OrderModel> orderModels)
    {
        this.orderModels = orderModels;
        this.orderService = new LocalOrderService();
    }

    public LocalOrderThread(OrderModel orderModel)
    {
        this.orderModels.add(orderModel);
        this.orderService = new LocalOrderService();
    }

    public void run()
    {
        this.orderService.append(this.orderModels);
    }
}
