package com.mutuelle.controller;

import com.mutuelle.config.MutableClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/test/time")
@Profile({"test", "dev"})
public class TestTimeController {

    private final MutableClock clock;

    @Autowired
    public TestTimeController(MutableClock clock) {
        this.clock = clock;
    }

    @GetMapping("/current")
    public Map<String, Object> getCurrentTime() {
        return Map.of(
            "currentTime", LocalDateTime.now(clock).toString(),
            "isMocked", true
        );
    }

    @PostMapping("/advance")
    public Map<String, Object> advanceTime(@RequestBody Map<String, Integer> request) {
        int days = request.getOrDefault("days", 0);
        int months = request.getOrDefault("months", 0);
        
        Duration duration = Duration.ofDays(days + (long) months * 30);
        clock.addTime(duration);
        
        return Map.of(
            "message", "Time advanced successfully",
            "newTime", LocalDateTime.now(clock).toString()
        );
    }

    @PostMapping("/set")
    public Map<String, Object> setTime(@RequestBody Map<String, String> request) {
        String isoDateTime = request.get("dateTime");
        LocalDateTime ldt = LocalDateTime.parse(isoDateTime);
        clock.setTime(ldt.atZone(ZoneId.systemDefault()).toInstant());
        
        return Map.of(
            "message", "Time set successfully",
            "newTime", LocalDateTime.now(clock).toString()
        );
    }
}
