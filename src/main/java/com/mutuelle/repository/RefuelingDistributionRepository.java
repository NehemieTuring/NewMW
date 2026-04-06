package com.mutuelle.repository;

import com.mutuelle.entity.RefuelingDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefuelingDistributionRepository extends JpaRepository<RefuelingDistribution, Long> {
    List<RefuelingDistribution> findByRefuelingId(Long refuelingId);
    List<RefuelingDistribution> findByMemberId(Long memberId);
}
