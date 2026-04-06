package com.mutuelle.repository;

import com.mutuelle.entity.InterestDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestDistributionRepository extends JpaRepository<InterestDistribution, Long> {
    List<InterestDistribution> findBySessionId(Long sessionId);
}
