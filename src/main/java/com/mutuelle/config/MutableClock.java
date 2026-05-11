package com.mutuelle.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;

/**
 * Horloge mutable pour le développement et les tests.
 * Le temps avance normalement (en temps réel) mais avec un décalage configurable.
 * Exemple : si on ajoute +5 jours, l'horloge affiche "maintenant + 5 jours" 
 * et continue de tic-tac normalement.
 */
public class MutableClock extends Clock {
    private final ZoneId zone;
    private Duration offset = Duration.ZERO;

    public MutableClock(Instant ignored, ZoneId zone) {
        this.zone = zone;
    }

    /**
     * Ajouter/retirer du temps au décalage actuel.
     */
    public void addTime(Duration duration) {
        this.offset = this.offset.plus(duration);
    }

    /**
     * Fixer l'horloge à un instant précis.
     * Calcule le décalage par rapport à l'heure réelle.
     */
    public void setTime(Instant targetInstant) {
        this.offset = Duration.between(Instant.now(), targetInstant);
    }

    /**
     * Réinitialiser le décalage à zéro (retour à l'heure réelle).
     */
    public void reset() {
        this.offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        MutableClock copy = new MutableClock(null, zone);
        copy.offset = this.offset;
        return copy;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(offset);
    }
}
