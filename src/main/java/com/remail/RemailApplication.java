package com.remail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RemailApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemailApplication.class, args);
    }
}