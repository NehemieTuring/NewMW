package com.mutuelle.repository;

import com.mutuelle.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByActiveTrue();
    Optional<Exercise> findByYear(String year);
}
