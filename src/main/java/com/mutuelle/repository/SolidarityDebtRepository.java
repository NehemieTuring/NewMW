package com.mutuelle.repository;

import com.mutuelle.entity.SolidarityDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolidarityDebtRepository extends JpaRepository<SolidarityDebt, Long> {
    Optional<SolidarityDebt> findByMemberId(Long memberId);
}
