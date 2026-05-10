package com.mutuelle.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;

public class MutableClock extends Clock {
    private Instant baseInstant;
    private final ZoneId zone;
    private Duration offset = Duration.ZERO;

    public MutableClock(Instant baseInstant, ZoneId zone) {
        this.baseInstant = baseInstant;
        this.zone = zone;
    }

    public void addTime(Duration duration) {
        this.offset = this.offset.plus(duration);
    }

    public void setTime(Instant newInstant) {
        this.baseInstant = newInstant;
        this.offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(baseInstant, zone);
    }

    @Override
    public Instant instant() {
        return baseInstant.plus(offset);
    }
}
