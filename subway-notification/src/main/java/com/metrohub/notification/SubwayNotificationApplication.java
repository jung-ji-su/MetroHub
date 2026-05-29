package com.metrohub.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SubwayNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubwayNotificationApplication.class, args);
    }
}
