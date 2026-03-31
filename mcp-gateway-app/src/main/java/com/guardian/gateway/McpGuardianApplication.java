package com.guardian.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.guardian")
public class McpGuardianApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpGuardianApplication.class, args);
    }
}
