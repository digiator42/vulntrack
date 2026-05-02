package com.vulntrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VuluntrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(VuluntrackApplication.class, args);
    }
}
