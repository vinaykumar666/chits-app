package com.ygc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class YgcInternalApplication {
    public static void main(String[] args) {
        SpringApplication.run(YgcInternalApplication.class, args);
    }
}
