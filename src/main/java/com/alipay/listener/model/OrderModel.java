package com.alipay.listener.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderModel {

    private String id;
    private Double amount;
    private String account;
    private String name;
    private String createdTime;
}
