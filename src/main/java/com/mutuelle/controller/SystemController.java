package com.mutuelle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mutuelle.config.MutableClock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Endpoints pour les informations système")
public class SystemController {

    private final Clock clock;

    @GetMapping("/time")
    @Operation(summary = "Récupérer la date et l'heure actuelles utilisées par l'API")
    public ResponseEntity<Map<String, Object>> getCurrentTime() {
        LocalDateTime now = LocalDateTime.now(clock);
        return ResponseEntity.ok(Map.of(
            "dateTime", now.toString(),
            "timestamp", now.atZone(clock.getZone()).toInstant().toEpochMilli(),
            "timezone", clock.getZone().toString(),
            "formatted", now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        ));
    }

    @PostMapping("/time")
    @Operation(summary = "Modifier l'heure système (uniquement en mode dev/test)")
    public ResponseEntity<Map<String, Object>> setSystemTime(@RequestBody Map<String, String> body) {
        try {
            if (!(clock instanceof MutableClock)) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'horloge n'est pas modifiable dans ce profil. Veuillez vérifier que ClockConfig injecte bien un MutableClock."));
            }

            MutableClock mutableClock = (MutableClock) clock;
            
            if (body.containsKey("isoDateTime")) {
                // Exemple: "2026-05-10T15:30:00"
                LocalDateTime newTime = LocalDateTime.parse(body.get("isoDateTime"));
                mutableClock.setTime(newTime.atZone(clock.getZone()).toInstant());
            } else if (body.containsKey("addHours")) {
                mutableClock.addTime(Duration.ofHours(Long.parseLong(body.get("addHours"))));
            } else if (body.containsKey("addDays")) {
                mutableClock.addTime(Duration.ofDays(Long.parseLong(body.get("addDays"))));
            } else if (body.containsKey("reset")) {
                mutableClock.reset();
            }

            return getCurrentTime();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Erreur lors de la modification de l'heure",
                "details", e.getMessage()
            ));
        }
    }
}
