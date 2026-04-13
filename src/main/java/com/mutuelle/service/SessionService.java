package com.mutuelle.service;

import com.mutuelle.entity.Exercise;
import com.mutuelle.entity.Session;
import com.mutuelle.enums.SessionState;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ExerciseService exerciseService;

    @Transactional
    public Session createSession(Session session) {
        Exercise targetExercise;
        if (session.getExercise() != null && session.getExercise().getId() != null) {
             targetExercise = exerciseService.getAllExercises().stream()
                     .filter(e -> e.getId().equals(session.getExercise().getId()))
                     .findFirst()
                     .orElseThrow(() -> new BusinessException("Exercice spécifié introuvable"));
        } else {
             targetExercise = exerciseService.getActiveExercise();
        }
        
        // Deactivate old active session
        sessionRepository.findByActiveTrue().ifPresent(s -> {
            s.setActive(false);
            sessionRepository.save(s);
        });

        session.setExercise(targetExercise);
        session.setActive(true);
        session.setState(SessionState.OPEN);
        return sessionRepository.save(session);
    }

    @Transactional
    public Session closeSession(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Session introuvable"));
        
        session.setState(SessionState.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        session.setActive(false);
        return sessionRepository.save(session);
    }

    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    public Session getActiveSession() {
        return sessionRepository.findByActiveTrue()
                .orElseThrow(() -> new BusinessException("Aucune session active trouvée"));
    }

    @Transactional
    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }

    @Transactional
    public Session configureSession(Long id, java.util.Map<String, Object> config) {
        Session session = sessionRepository.findById(id).orElseThrow();
        // Dynamic configuration logic could be added here
        return sessionRepository.save(session);
    }
}
