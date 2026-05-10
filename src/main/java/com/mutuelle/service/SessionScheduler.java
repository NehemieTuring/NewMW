package com.mutuelle.service;

import com.mutuelle.entity.Session;
import com.mutuelle.enums.SessionState;
import com.mutuelle.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionScheduler {

    private final SessionRepository sessionRepository;
    private final Clock clock;

    /**
     * Chaque heure, vérifie s'il y a des sessions ouvertes dont la date est passée.
     * Si oui, les clôture automatiquement.
     */
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures au début de l'heure
    @Transactional
    public void autoCloseExpiredSessions() {
        LocalDate today = LocalDate.now(clock);
        log.info("Vérification des sessions expirées le {}", today);

        List<SessionState> openStates = Arrays.asList(SessionState.OPEN, SessionState.SAVING);
        List<Session> expiredSessions = sessionRepository.findByDateBeforeAndStateNotIn(today, Arrays.asList(SessionState.CLOSED, SessionState.ARCHIVED));

        for (Session session : expiredSessions) {
            log.info("Clôture automatique de la session {} (Date: {}, État: {})", session.getId(), session.getDate(), session.getState());
            session.setState(SessionState.CLOSED);
            session.setActive(false);
            session.setClosedAt(LocalDateTime.now(clock));
            sessionRepository.save(session);
        }
        
        if (!expiredSessions.isEmpty()) {
            log.info("{} sessions ont été clôturées automatiquement.", expiredSessions.size());
        }
    }
}
