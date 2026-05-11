package com.mutuelle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    @Primary
    public MutableClock clock() {
        // MutableClock avec offset=0 se comporte exactement comme Clock.systemDefaultZone()
        // mais permet la modification via l'endpoint /system/time en dev
        return new MutableClock(Instant.now(), ZoneId.systemDefault());
    }
}
