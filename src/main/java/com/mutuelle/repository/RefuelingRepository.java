package com.mutuelle.repository;

import com.mutuelle.entity.Refueling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefuelingRepository extends JpaRepository<Refueling, Long> {
    Optional<Refueling> findByExerciseId(Long exerciseId);
}
