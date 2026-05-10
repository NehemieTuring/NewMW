package com.mutuelle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    @Primary
    @Profile("!test & !dev")
    public Clock realClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Primary
    @Profile({"test", "dev"})
    public MutableClock mutableClock() {
        // En mode dev ou test, on commence à l'heure actuelle mais on permet la modification
        return new MutableClock(Instant.now(), ZoneId.systemDefault());
    }
}
