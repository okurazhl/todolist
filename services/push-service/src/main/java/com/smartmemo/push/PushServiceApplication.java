package com.smartmemo.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PushServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PushServiceApplication.class, args);
    }
}
