package com.quant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 股票量化交易系统 - 主启动类
 */
@SpringBootApplication
@EnableScheduling
public class QuantSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantSystemApplication.class, args);
        System.out.println("======================================");
        System.out.println("  Quant System Started Successfully!");
        System.out.println("  Module 1: Data Collection Layer");
        System.out.println("======================================");
    }
}