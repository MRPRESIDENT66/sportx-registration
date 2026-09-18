package com.jinmingyi.flashregistration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.jinmingyi.flashregistration.mapper")
@SpringBootApplication
public class FlashRegistrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashRegistrationApplication.class, args);
    }
}
