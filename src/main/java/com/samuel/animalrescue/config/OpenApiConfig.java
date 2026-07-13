package com.samuel.animalrescue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI animalRescueOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Animal Rescue Maze API")
                .version("1.0.0")
                .description("Guest-play maze sessions, authoritative movement, scoring and leaderboard."));
    }

    @Bean
    Clock gameClock() {
        return Clock.systemUTC();
    }
}
