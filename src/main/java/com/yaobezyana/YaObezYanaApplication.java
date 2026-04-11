package com.yaobezyana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YaObezYanaApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaObezYanaApplication.class, args);
    }
}
