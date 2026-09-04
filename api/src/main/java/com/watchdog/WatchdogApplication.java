package com.watchdog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Watchdog — continuous ATS job-hunting agent.
 * Entry point for the Spring Boot application.
 */
@SpringBootApplication
public class WatchdogApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatchdogApplication.class, args);
    }
}
