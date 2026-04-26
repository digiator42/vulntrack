package com.vulntrack.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Configuration
@Profile("dev") // Only runs during local development
public class SeedDataConfig {

    @Bean
    CommandLineRunner run(DataInitializer initializer) {
        return args -> {
            try {
                initializer.init();
            } catch (Exception e) {
                System.err.println("Data seeding failed: " + e.getMessage());
            }
        };
    }
}