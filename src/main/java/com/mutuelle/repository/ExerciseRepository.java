package com.mutuelle.repository;

import com.mutuelle.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByActiveTrue();
    Optional<Exercise> findByYear(String year);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Exercise e SET e.active = false")
    void deactivateAll();
}
