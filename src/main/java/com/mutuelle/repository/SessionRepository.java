package com.mutuelle.repository;

import com.mutuelle.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByActiveTrue();
    List<Session> findByExerciseId(Long exerciseId);
    List<Session> findByDateBeforeAndStateNotIn(java.time.LocalDate date, java.util.Collection<com.mutuelle.enums.SessionState> states);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.sessionNumber) FROM Session s WHERE s.exercise.id = :exerciseId")
    Integer findMaxSessionNumberByExerciseId(Long exerciseId);
}
